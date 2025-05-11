try {
    var clientPort = 8080
    config.devServer.https = true
    config.devServer.proxy = {
        '/api/ws/**': {
            target: 'ws://127.0.0.1:' + clientPort + '/',
            ws: true
        },
        '/api/**': {
            target: 'http://127.0.0.1:' + clientPort, pathRewrite: function (path, req) {
                // return path;
                return path;//.substring(4);
            },
            headers: {
                Connection: 'close'
            }
        }
    }
    config.devServer.historyApiFallback = {
        index: 'index.html',
        rewrites: [
            {"from": "output.css", "to": "/output.css"}
        ]
    }
} catch (e) {
}