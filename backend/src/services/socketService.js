const socketIo = require('socket.io');

let io;

module.exports = {
    init: (server) => {
        io = socketIo(server, {
            cors: {
                origin: '*',
                methods: ['GET', 'POST']
            }
        });

        io.on('connection', (socket) => {
            console.log('Cliente conectado:', socket.id);

            socket.on('entregador:online', (data) => {
                socket.broadcast.emit('entregador:online', data);
            });

            socket.on('entregador:offline', (data) => {
                socket.broadcast.emit('entregador:offline', data);
            });

            socket.on('disconnect', () => {
                console.log('Cliente desconectado:', socket.id);
            });
        });

        return io;
    },

    getIO: () => {
        if (!io) {
            throw new Error('Socket.io não inicializado!');
        }
        return io;
    }
};
