const { PDFDocument, rgb, StandardFonts } = require('pdf-lib');

exports.gerarPdfEstoque = async (itens) => {
    const pdfDoc = await PDFDocument.create();
    let page = pdfDoc.addPage([600, 800]);
    const { width, height } = page.getSize();
    const font = await pdfDoc.embedFont(StandardFonts.Helvetica);
    const fontBold = await pdfDoc.embedFont(StandardFonts.HelveticaBold);

    const drawHeader = (p) => {
        p.drawText('Relatório de Estoque - Conexão BR277', {
            x: 50,
            y: height - 50,
            size: 18,
            font: fontBold,
            color: rgb(0, 0, 0.5),
        });

        p.drawText(`Gerado em: ${new Date().toLocaleString('pt-BR')}`, {
            x: 50,
            y: height - 70,
            size: 10,
            font: font,
        });

        // Cabeçalho da Tabela
        const tableY = height - 100;
        p.drawRectangle({ x: 45, y: tableY - 5, width: 510, height: 20, color: rgb(0.9, 0.9, 0.9) });
        p.drawText('Produto', { x: 50, y: tableY, size: 10, font: fontBold });
        p.drawText('Qtd', { x: 300, y: tableY, size: 10, font: fontBold });
        p.drawText('Estocagem', { x: 350, y: tableY, size: 10, font: fontBold });
        p.drawText('Venda', { x: 480, y: tableY, size: 10, font: fontBold });
        return tableY - 25;
    };

    let currentY = drawHeader(page);

    for (const item of itens) {
        if (currentY < 50) {
            page = pdfDoc.addPage([600, 800]);
            currentY = drawHeader(page);
        }

        page.drawText(item.nome || '', { x: 50, y: currentY, size: 9, font: font });
        page.drawText(`${item.quantidade || 0}`, { x: 300, y: currentY, size: 9, font: font });
        page.drawText(item.modo_estocagem || '-', { x: 350, y: currentY, size: 9, font: font });

        const preco = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(item.preco_venda || 0);
        page.drawText(preco, { x: 480, y: currentY, size: 9, font: font });

        currentY -= 20;
        page.drawLine({
            start: { x: 45, y: currentY + 15 },
            end: { x: 555, y: currentY + 15 },
            thickness: 0.5,
            color: rgb(0.8, 0.8, 0.8),
        });
    }

    const pdfBytes = await pdfDoc.save();
    return pdfBytes;
};
