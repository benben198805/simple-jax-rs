package simple.jax.rs;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;

public class JettyServer {
    private Server server;
    private Class[] resources;

    public JettyServer(Class[] resources) {
        this.resources = resources;
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("jetty-server");

        server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);

        server.addConnector(connector);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                JettyServer.this.handle(request, response, JettyServer.this.resources);
                baseRequest.setHandled(true);
            }


        });
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response, Class[] resources) {
        HandleTest.Dispatcher dispatcher = new HandleTest.Dispatcher(new HandleTest.DispatcherTable(resources[0]));
        dispatcher.handle(request, response);

    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
