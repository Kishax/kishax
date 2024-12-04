package keyp.forev.fmc.velocity.discord;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;

import keyp.forev.fmc.velocity.discord.interfaces.ReflectionHandler;

import java.lang.reflect.InvocationHandler;

public class DynamicEventRegister {

    public static CompletableFuture<Object> registerListeners(Object target, Object jdaBuilder, URLClassLoader jdaURLClassLoader) throws Exception {
        Class<?> eventListenerClass = Class.forName("net.dv8tion.jda.api.hooks.ListenerAdapter", true, jdaURLClassLoader);
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ReflectionHandler.class)) {
                ReflectionHandler annotation = method.getAnnotation(ReflectionHandler.class);
                Class<?> eventClass = Class.forName(annotation.event(), true, jdaURLClassLoader);
                Object proxy = Proxy.newProxyInstance(
                    eventListenerClass.getClassLoader(),
                    new Class<?>[]{eventListenerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method eventMethod, Object[] args) throws Throwable {
                            if (args != null && args.length == 1 && eventClass.isInstance(args[0])) {
                                method.invoke(target, args[0]);
                            }
                            return null;
                        }
                    }
                );
                Method addEventListenerMethod = jdaBuilder.getClass().getMethod("addEventListener", eventListenerClass);
                jdaBuilder = addEventListenerMethod.invoke(jdaBuilder, proxy);
            }
        }
        return CompletableFuture.completedFuture(jdaBuilder);
    }
}
