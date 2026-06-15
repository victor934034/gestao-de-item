const fs = require('fs');
const pdf = require('pdf-parse');

const PATTERNS = {
    numeroPedido: /PEDIDO\s+Nº\s*([\d.]+)/i,
    dataPedido: /Data:\s*(\d{2}\/\d{2}\/\d{4}|\d{2}\/\d{2}\/\d{2}|\d{2}\/[A-Za-z]+\/\d{4})/i,
    dataEntrega: /(?:Previsão Entrega|Data Entrega|Entregar em):\s*(\d{2}\/\d{2}\/\d{4})/i,
    horaEntrega: /(?:Hora Entrega|Horário):\s*(\d{2}:\d{2})/i,
    nomeCliente: /Nome:\s*(?:\d+[-]\s*)?([^/\n\r\d(]+?)(?:\s+-\s+|\s*[/]|Fone:|$)/i,
    formaPagamento: /Forma Pg\n(?:.*\n){3}([A-ZÀ-Ú\s]+)\n/i,
    vencimento: /\(\d\)(\d{2}\/\d{2}\/\d{2})/i,
    telefone: /(?:Fone|Celular):\s*([\d\s()-]+?)(?=\s*(?:E-mail|CPF|CNPJ|Endere|CEP|Inscrição|\n|$))/i,
    email: /(?:E-mail|Email):\s*([\w.-]+@[\w.-]+\.[a-zA-Z]{2,})/i
};

function extractField(text, regex, defaultValue = '') {
    const match = text.match(regex);
    if (match && match[1]) {
        let val = match[1].trim();
        if (regex === PATTERNS.nomeCliente) {
            val = val.replace(/^[\d\s-]+/, '').trim();
        }
        return { value: val, confianca: 'alta' };
    }
    return { value: defaultValue, confianca: 'baixa' };
}

function extractItems(text) {
    const items = [];
    const lines = text.split('\n');
    lines.forEach(line => {
        const itemLineRegex = /^(\d+)\s+(.+?)\s+(\d+[\d,.]*)(pç|M²|un|kg|mt|cx|rol|par)/i;
        const m = line.trim().match(itemLineRegex);
        if (m) {
            items.push({
                idx: parseInt(m[1]),
                descricao: m[2].trim(),
                quantidade: parseFloat(m[3].replace(',', '.')),
                unidade: m[4]
            });
        }
    });
    return items;
}

function extractTotalLiquido(text) {
    const totalLiquidoPos = text.indexOf('Total Liquido R$');
    if (totalLiquidoPos !== -1) {
        const afterTotal = text.substring(totalLiquidoPos);
        // Tenta capturar o último valor monetário se houver concatenação (como visto no log)
        const matches = afterTotal.match(/\d+,\d{2}/g);
        if (matches && matches.length > 0) {
            // No caso de arquivoapp.pdf, o Total Liquido costuma ser o último valor do bloco ou o repetido no Vencto
            // Vamos procurar especificamente por um valor que se repita no final do arquivo (pagamento)
            const lastMatch = matches[matches.length - 1];
            return { value: lastMatch, confianca: 'alta' };
        }
    }
    // Fallback: procura o valor antes do rodapé ou perto das formas de pagamento
    const paymentMatch = text.match(/(?:Valor R\$|DINHEIRO|CARTÃO|PIX|BOLETO|TRANSFERÊNCIA)[\s\S]{0,100}(\d+,\d{2})/i);
    if (paymentMatch) {
        return { value: paymentMatch[1], confianca: 'alta' };
    }

    const allMatches = text.match(/\d+,\d{2}/g);
    if (allMatches && allMatches.length > 0) {
        return { value: allMatches[allMatches.length - 1], confianca: 'baixa' };
    }
    return { value: '0,00', confianca: 'baixa' };
}

function extractTelefoneCliente(text) {
    const blockMatch = text.match(/Nome:[\s\S]{1,300}/i);
    if (blockMatch) {
        const block = blockMatch[0];
        const foneRegex = /(?:\(?\d{2}\)?\s*)?9?\d{4,5}[-\s]*\d{4}/g;
        const potentialFones = block.match(foneRegex);
        if (potentialFones) {
            for (const fone of potentialFones) {
                const num = fone.replace(/\D/g, '');
                if (!num.includes('995278067') && num.length >= 8) {
                    return { value: fone.trim(), confianca: 'alta' };
                }
            }
        }
    }
    const matches = [...text.matchAll(/Fone:\s*([\d\s()-]+?)(?=\s*(?:E-mail|CPF|CNPJ|Endere|CEP|Inscrição|\n|$))/gi)];
    for (const m of matches) {
        let t = m[1].trim();
        let nums = t.replace(/\D/g, '');
        if (!nums.includes('995278067') && nums.length >= 8) {
            return { value: t, confianca: 'média' };
        }
    }
    return { value: '', confianca: 'baixa' };
}

function decomporEndereco(text) {
    let textoParaProcessar = '';
    let foundMarker = false;

    // 1. Tenta por marcador explícito (Pedido Único)
    if (text.includes('Endereço de Entrega:')) {
        // Regex mais específica para capturar a linha IMEDIATAMENTE após o rótulo
        const match = text.match(/Endereço de Entrega:\s*[\r\n]+([\s\S]+?)(?=Aprovação|Vendedor|Transportador|Condição|Vencimento|Data\/Hora|$)/i);
        if (match) {
            textoParaProcessar = match[1].split(/[\r\n]/)[0].trim();
            foundMarker = true;
        }
    }

    // 2. Tenta por fallback de Telefone (Outros formatos de pedido único)
    if (!foundMarker) {
        const fallMatch = text.match(/(?:Telefone|Fone|Celular):.*?(?:\r|\n)([\s\S]+?)(?:Previsão Entrega|Data Entrega|Entregar em|T\.Valor R\$|Forma Pg|Vendedor|Condição|DINHEIRO|CARTÃO|PIX|BOLETO)/i);
        if (fallMatch) {
            textoParaProcessar = fallMatch[1].trim();
            foundMarker = true;
        }
    }

    // 3. Se não achou marcador, mas o texto foi passado (ex: do lote), valida se não é lixo
    if (!foundMarker && text.trim().length > 5) {
        const clean = text.trim();
        // Se começar com palavras de pagamento, provavelmente não é endereço
        const isNotAddress = /^(DINHEIRO|CARTÃO|PIX|BOLETO|TRANSFERÊNCIA|T\.VALOR|VALOR|TOTAL|ITEM|CÓDIGO)/i.test(clean);
        if (!isNotAddress) {
            textoParaProcessar = clean;
        }
    }

    if (!textoParaProcessar) {
        return { endereco: '', numero: '', bairro: '', observacao: '', original: '' };
    }

    const original = textoParaProcessar;
    let logradouro = '';
    let numero = '';
    let bairro = '';
    let observacao = '';

    const avisoRegex = /\(([^)]+)\)|(?:PRÓX|PERTO|AO LADO|EM FRENTE|PORTÃO|CASA DE COR|CASA COR|MURO)\s+[^,-]+/gi;
    const avisosEncontrados = original.match(avisoRegex);

    let tempText = original;
    if (avisosEncontrados) {
        observacao = avisosEncontrados.join('; ');
        avisosEncontrados.forEach(a => {
            tempText = tempText.replace(a, '');
        });
    }

    // Processamento de partes (Rua, Numero, Bairro)
    const cleanParts = tempText.split('-').map(p => p.trim()).filter(p => p.length > 0);

    if (cleanParts.length >= 1) {
        const ruaNum = cleanParts[0].split(',');
        logradouro = ruaNum[0].trim();
        if (ruaNum.length > 1) {
            numero = ruaNum[1].trim();
        }
    }

    if (cleanParts.length >= 2) {
        const p2 = cleanParts[1].toUpperCase();
        if (p2.includes('CASA') || p2.includes('APTO') || p2.includes('LOJA') || p2.includes('BLOCO')) {
            if (!observacao.includes(cleanParts[1])) {
                observacao = observacao ? `${observacao}; ${cleanParts[1]}` : cleanParts[1];
            }
            if (cleanParts.length >= 3) {
                bairro = cleanParts[cleanParts.length - 1].trim();
            }
        } else {
            bairro = cleanParts[1].trim();
        }
    } else if (!bairro && cleanParts.length === 1 && !numero) {
        // Fallback para quando o número está no fim da string sem vírgula
        const numMatch = logradouro.match(/(.*)\s+(\d+)$/);
        if (numMatch) {
            logradouro = numMatch[1].trim();
            numero = numMatch[2].trim();
        }
    }

    if (!numero && logradouro.includes(',')) {
        const spl = logradouro.split(',');
        logradouro = spl[0].trim();
        numero = spl[1].trim();
    }

    logradouro = logradouro.replace(/[(),]/g, '').trim();
    numero = numero.replace(/[(),]/g, '').trim();

    return { endereco: logradouro, numero, bairro, observacao, original };
}

async function parsePedidoPdf(filePath) {
    try {
        const dataBuffer = fs.readFileSync(filePath);
        const data = await pdf(dataBuffer);
        const text = data.text;

        if (text.includes('L I S T A   DE   E N T R E G A   E   C O B R A N Ç A')) {
            return await parseListaEntrega(text);
        }

        if (text.includes('LISTA DE ITENS - ORÇAMENTOS')) {
            return await parseOrcamentoLote(text);
        }

        const endereco = decomporEndereco(text);
        const parsedData = {
            isMulti: false,
            numeroPedido: extractField(text, PATTERNS.numeroPedido, '000'),
            dataPedido: extractField(text, PATTERNS.dataPedido, ''),
            dataEntregaProgramada: extractField(text, PATTERNS.dataEntrega, ''),
            horaEntregaProgramada: extractField(text, PATTERNS.horaEntrega, ''),
            nomeCliente: extractField(text, PATTERNS.nomeCliente, 'Cliente não identificado'),
            telefoneCliente: extractTelefoneCliente(text),
            emailCliente: extractField(text, PATTERNS.email, ''),
            totalLiquido: extractTotalLiquido(text),
            formaPagamento: extractField(text, PATTERNS.formaPagamento, ''),
            vencimento: extractField(text, PATTERNS.vencimento, ''),
            endereco: {
                endereco: endereco.endereco,
                numero: endereco.numero,
                bairro: endereco.bairro,
                observacao: endereco.observacao,
                original: endereco.original
            },
            itens: extractItems(text)
        };

        return parsedData;

    } catch (error) {
        console.error('Erro ao processar PDF:', error.message);
        throw new Error('Falha no processamento: ' + error.message);
    }
}

async function parseOrcamentoLote(text) {
    const orders = [];
    // Split by "Cliente:" but keep the marker using lookahead
    const blocks = text.split(/(?=Cliente:)/);

    // The first block might be the header "LISTA DE ITENS - ORÇAMENTOS"
    blocks.forEach(block => {
        if (!block.includes('Cliente:')) return;

        const lines = block.split('\n').map(l => l.trim()).filter(l => l.length > 0);
        const pedido = {
            numeroPedido: { value: 'ORC-' + Math.random().toString(36).substr(2, 6).toUpperCase(), confianca: 'baixa' },
            dataPedido: { value: new Date().toLocaleDateString('pt-BR'), confianca: 'baixa' },
            nomeCliente: { value: '', confianca: 'baixa' },
            telefoneCliente: { value: '', confianca: 'baixa' },
            emailCliente: { value: '', confianca: 'baixa' },
            totalLiquido: { value: '0,00', confianca: 'baixa' },
            formaPagamento: { value: 'A combinar', confianca: 'baixa' },
            endereco: { endereco: '', numero: '', bairro: '', observacao: '' },
            itens: [],
            isFromOrcamento: true
        };

        // Extract Client Name
        const clienteMatch = block.match(/Cliente:\s*([^\n\r]+)/i);
        if (clienteMatch) {
            pedido.nomeCliente = { value: clienteMatch[1].trim(), confianca: 'alta' };
        }

        // Logic for Item Extraction
        // Items look like: "741PCRipado 16x290 Pinus35,0058,90"
        // Pattern: [Qtd][Un][Description][VlUnit][Total]
        // Prices usually have two decimals at the end
        lines.forEach(line => {
            if (line.includes('Cliente:') || line.includes('QtdUn.') || line.includes('Total')) return;

            // Try to match the dense format: (Qtd)(Un)(Description)(Price1,Price2)
            // Example: "741PCRipado 16x290 Pinus35,0058,90"
            // Case 1: Triple digit + PC/DC + text + prices
            const denseMatch = line.match(/^(\d+)(PC|DC|UN|KG|MT|PR|CX|UN|PÇ)?(.+?)(\d+,\d{2})?(\d+,\d{2})?$/i);

            if (denseMatch) {
                const qtd = denseMatch[1];
                const un = denseMatch[2] || 'un';
                let desc = denseMatch[3].trim();
                const total = denseMatch[5] || denseMatch[4] || '0,00';

                // Cleanup description from numeric prefixes if it was matched in desc
                desc = desc.replace(/^\d+/, '').trim();

                if (desc.length > 2 && !/Solicitar|Danificado/i.test(desc)) {
                    pedido.itens.push({
                        idx: pedido.itens.length + 1,
                        descricao: desc,
                        quantidade: parseFloat(qtd),
                        unidade: un.toLowerCase()
                    });
                }
            } else {
                // Secondary match for less dense lines: "2Brize Tabaco54,00"
                const simpleMatch = line.match(/^(\d+)(.+?)(\d+,\d{2})?$/);
                if (simpleMatch) {
                    const qtd = simpleMatch[1];
                    let desc = simpleMatch[2].trim();
                    if (desc.length > 2 && !/Solicitar|Danificado/i.test(desc)) {
                        pedido.itens.push({
                            idx: pedido.itens.length + 1,
                            descricao: desc,
                            quantidade: parseFloat(qtd),
                            unidade: 'un'
                        });
                    }
                }
            }
        });

        if (pedido.itens.length > 0) {
            orders.push(pedido);
        }
    });

    return { isMulti: true, pedidos: orders };
}

async function parseListaEntrega(text) {
    const orders = [];

    const boundaryPoints = [];
    const boundaryRegex = /([\n\r]|^)\s{0,20}\d{1,3}\s{1,20}(\d{5,})/g;
    let match;
    while ((match = boundaryRegex.exec(text)) !== null) {
        boundaryPoints.push({ index: match.index, nf: match[2] });
    }

    if (boundaryPoints.length === 0) {
        const aggressiveRegex = /\d{1,3}\s{1,20}(\d{5,})/ig;
        while ((match = aggressiveRegex.exec(text)) !== null) {
            boundaryPoints.push({ index: match.index, nf: match[1] });
        }
    }

    for (let i = 0; i < boundaryPoints.length; i++) {
        const start = boundaryPoints[i].index;
        const end = boundaryPoints[i + 1] ? boundaryPoints[i + 1].index : text.length;
        const originalBlock = text.substring(start, end);
        const normalizedBlock = originalBlock.replace(/\s+/g, ' ');

        const pedido = {
            numeroPedido: { value: boundaryPoints[i].nf, confianca: 'alta' },
            dataPedido: { value: '', confianca: 'baixa' },
            nomeCliente: { value: '', confianca: 'baixa' },
            telefoneCliente: { value: '', confianca: 'baixa' },
            emailCliente: { value: '', confianca: 'baixa' },
            totalLiquido: { value: '0,00', confianca: 'baixa' },
            formaPagamento: { value: '', confianca: 'baixa' },
            endereco: { endereco: '', numero: '', bairro: '', observacao: '' },
            itens: [],
            isFromList: true
        };

        const dateMatch = normalizedBlock.match(/(\d{2}\/\d{2}\/\d{4})/);
        if (dateMatch) {
            pedido.dataPedido = { value: dateMatch[1], confianca: 'alta' };
            const afterDate = normalizedBlock.substring(dateMatch.index + dateMatch[0].length).trim();
            // Forma de pagamento geralmente está após a data, ou no final. Vamos ser mais flexíveis
            const pgtoMatch = afterDate.match(/(Dinheiro|Cartão|Pix|Boleto|Transferência[a-zA-ZÀ-Ú\s]*)\b/i);
            if (pgtoMatch) {
                pedido.formaPagamento = { value: pgtoMatch[0].trim(), confianca: 'alta' };
            } else {
                const generalPgto = afterDate.match(/^[A-ZÀ-Ú\s]{3,20}\b/);
                if (generalPgto && !generalPgto[0].includes('T.Valor')) {
                    pedido.formaPagamento = { value: generalPgto[0].trim(), confianca: 'média' };
                }
            }
        }

        const moneyMatches = [...normalizedBlock.matchAll(/(\d[\d.]*,\d{2})/g)];
        if (moneyMatches.length > 0) {
            // No modo Lote (Lista), o Total costuma estar na linha do cabeçalho, logo após Forma Pgto
            // Vamos tentar achar o valor que vem após DINHEIRO/CARTAO etc no bloco
            const headerValueMatch = normalizedBlock.match(/(?:DINHEIRO|CARTÃO|PIX|BOLETO|TRANSFERÊNCIA)\s+(\d+,\d{2})/i);
            if (headerValueMatch) {
                pedido.totalLiquido = { value: headerValueMatch[1], confianca: 'alta' };
            } else {
                pedido.totalLiquido = { value: moneyMatches[0][1], confianca: 'média' }; // Primeiro costuma ser o total na linha 20/22
            }
        }

        let headerText = '';
        if (dateMatch) {
            headerText = normalizedBlock.split(pedido.numeroPedido.value)[1].split(dateMatch[1])[0].trim();
        } else {
            headerText = normalizedBlock.split(pedido.numeroPedido.value)[1];
            if (headerText) {
                if (headerText.indexOf('T.Valor') !== -1) {
                    headerText = headerText.split('T.Valor')[0].trim();
                }
            } else {
                headerText = '';
            }
        }

        if (headerText.includes(' - ')) {
            const p = headerText.split(' - ');
            pedido.nomeCliente = { value: p[0].trim(), confianca: 'alta' };
            pedido.telefoneCliente = { value: p.slice(1).join(' - ').trim(), confianca: 'alta' };
        } else {
            const foneEndMatch = headerText.match(/(.*?)\s+((?:\(?\d{2}\)?\s*)?9?\d{4,5}[-\s]*\d{4}.*)/);
            if (foneEndMatch) {
                pedido.nomeCliente = { value: foneEndMatch[1].trim(), confianca: 'alta' };
                pedido.telefoneCliente = { value: foneEndMatch[2].trim(), confianca: 'alta' };
            } else {
                pedido.nomeCliente = { value: headerText, confianca: 'média' };
            }
        }

        // Tentar extrair endereço no Multi-PDF (que fica entre o header e os itens)
        let possibleAddress = '';
        if (dateMatch) {
            possibleAddress = originalBlock.substring(originalBlock.indexOf(dateMatch[1]) + dateMatch[1].length);
            const tValorIdx = possibleAddress.indexOf('T.Valor R$');
            if (tValorIdx !== -1) {
                possibleAddress = possibleAddress.substring(0, tValorIdx);
            }
        }
        if (possibleAddress.length > 5) {
            const endDec = decomporEndereco(possibleAddress);
            pedido.endereco = {
                endereco: endDec.endereco,
                numero: endDec.numero,
                bairro: endDec.bairro,
                observacao: endDec.observacao
            };
        }

        // Itens: Usa o originalBlock pois tem newlines
        const itemsPrefix = "T.Valor R$";
        const itemsAreaIndex = originalBlock.indexOf(itemsPrefix);
        if (itemsAreaIndex !== -1) {
            const itemsArea = originalBlock.substring(itemsAreaIndex + itemsPrefix.length);
            const itemLines = itemsArea.split(/\n|\r/).map(l => l.trim()).filter(l => l.length > 5); // 5 chars minimo
            let idx = 1;
            itemLines.forEach(line => {
                // Regex mais flexível: captura quantidades decimais no final
                const flexRegex = /^(\d+)?\s*(.+?)\s+(\d+[,.]\d{2,3}|\d+)\s*(un|pç|kg|mt)?/i;
                const m = line.match(flexRegex);

                // fallback pra formato denso
                const dense = line.match(/^(\d{1})?(\d{5,})?(.+?)\.{3,}(\d+[,.]\d{3}),(\d{3})(\d+[,.]\d{2})$/);

                if (dense && dense[3]) {
                    pedido.itens.push({
                        idx: idx++,
                        descricao: dense[3].trim(),
                        quantidade: parseFloat(dense[4].replace(',', '.')),
                        unidade: 'un'
                    });
                } else if (m && m[2] && m[2].length > 3 && !m[2].includes('Total')) {
                    // Try to extract quantity from end if flex didn't catch properly in group 3
                    let desc = m[2].trim();
                    let qtd = 1;

                    const endQtd = line.match(/(\d+[,.]\d{2,4}|\d+)\s*(un|pç|kg|mt)?\s*(\d+[,.]\d{2})?$/i);
                    if (endQtd) {
                        qtd = parseFloat(endQtd[1].replace(',', '.'));
                    }

                    pedido.itens.push({
                        idx: idx++,
                        descricao: desc.replace(/\.+$/, '').trim(),
                        quantidade: qtd,
                        unidade: m[4] || 'un'
                    });
                }
            });
        }

        orders.push(pedido);
    }

    return { isMulti: true, pedidos: orders };
}

async function parseEstoquePdf(filePath) {
    try {
        const dataBuffer = fs.readFileSync(filePath);
        const data = await pdf(dataBuffer);
        const text = data.text;
        const rawLines = text.split(/\n|\r/).map(l => l.trim()).filter(l => l.length > 0);

        const items = [];
        const ignoreRegex = /estoque|inventário|relatório|página|total|solicitar|solicitação|lista de itens|qtdun\.|cliente:/i;

        let currentItem = null;

        rawLines.forEach((line) => {
            if (ignoreRegex.test(line)) return;

            // Pattern for a new item: starts with digit(s)
            // We just capture the quantity at the start.
            const itemStartMatch = line.match(/^(\d+)(.*)/);

            if (itemStartMatch && itemStartMatch[1].length < 10) {
                if (currentItem) {
                    processAndAddItem(currentItem, items);
                }

                currentItem = {
                    qtd: itemStartMatch[1],
                    content: itemStartMatch[2].trim()
                };
            } else if (currentItem) {
                currentItem.content += " " + line;
            }
        });

        if (currentItem) {
            processAndAddItem(currentItem, items);
        }

        function processAndAddItem(item, list) {
            let content = item.content.trim();

            // Extract prices from end of string
            // Cases: "35,00 58,90" or "R$ 35,00 R$ 58,90" or "54,22" (single price)
            const priceRegexDouble = /(?:R\$\s*)?(\d+(?:\.\d{3})*[,.]\d{2})\s*(?:R\$\s*)?(\d+(?:\.\d{3})*[,.]\d{2})$/;
            const priceRegexSingle = /(?:R\$\s*)?(\d+(?:\.\d{3})*[,.]\d{2})$/;

            let nome = content;
            let custo = 0;
            let venda = 0;

            const doubleMatch = content.match(priceRegexDouble);
            if (doubleMatch) {
                custo = parseFloat(doubleMatch[1].replace(/\./g, '').replace(',', '.'));
                venda = parseFloat(doubleMatch[2].replace(/\./g, '').replace(',', '.'));
                nome = content.substring(0, content.lastIndexOf(doubleMatch[0])).trim();
            } else {
                const singleMatch = content.match(priceRegexSingle);
                if (singleMatch) {
                    venda = parseFloat(singleMatch[1].replace(/\./g, '').replace(',', '.'));
                    nome = content.substring(0, content.lastIndexOf(singleMatch[0])).trim();
                }
            }

            // Detect unit (e.g., PC, UN, KG, MT, PR)
            // Units can be merged with the name (ex: PCRipado) or separated (ex: 2 Ripado)
            let unit = 'un';
            // Order matters: longer strings first or specific prefixes
            const possibleUnits = ['PC', 'UN', 'KG', 'MT', 'CX', 'PR', 'RL', 'DZ', 'PÇ', 'DC'];

            for (const u of possibleUnits) {
                // Regex check for unit at start, possibly followed by name or space
                // We use a boundary-like check: if it's merged, it's followed by a letter
                const uMatch = nome.match(new RegExp(`^(${u})(\\s+|[A-Z][a-z])`, 'i'));
                if (uMatch) {
                    unit = uMatch[1].toLowerCase();
                    // If matched via merged (group 2 starts with letter), we only cut the unit
                    nome = nome.substring(unit.length).trim();
                    break;
                }
            }

            // Final cleanup: remove residual codes or trailing dots
            nome = nome.replace(/^\d+[\s-.]*/, '').replace(/\.+$/, '').trim();

            if (nome.length >= 2 && !/solicitar|total/i.test(nome)) {
                list.push({
                    nome: nome,
                    quantidade: parseFloat(item.qtd),
                    modo_estocagem: unit,
                    custo: custo,
                    preco_venda: venda
                });
            }
        }

        return items;
    } catch (error) {
        console.error('Erro ao processar PDF de estoque:', error.message);
        throw new Error('Falha no processamento: ' + error.message);
    }
}

module.exports = {
    parsePedidoPdf,
    parseEstoquePdf
};
