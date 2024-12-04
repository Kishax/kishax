package keyp.forev.fmc.velocity.discord;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;

import keyp.forev.fmc.velocity.discord.interfaces.ReflectionHandler;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;

public class DynamicEventRegister {

    public static CompletableFuture<Object[]> registerListeners(
            DiscordEventListener listener, 
            URLClassLoader jdaURLClassLoader,
            Class<?> jdaBuilderClazz,
            Class<?> eventListenerClazz
        ) throws Exception {
        List<Object> proxys = new ArrayList<>();
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ReflectionHandler.class)) {
                ReflectionHandler annotation = method.getAnnotation(ReflectionHandler.class);
                Class<?> eventClazz = jdaURLClassLoader.loadClass(annotation.event());
                Object proxy = Proxy.newProxyInstance(
                    jdaURLClassLoader,
                    new Class<?>[]{eventListenerClazz},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method eventMethod, Object[] args) throws Throwable {
                            if (args != null && args.length == 1 && eventClazz.isInstance(args[0])) {
                                method.invoke(listener, args[0]);
                            }
                            return null;
                        }
                    }
                );
                proxys.add(proxy);
            }
        }
        
        // 可変長引数として渡すために配列をラップ
        //Object[] proxyArray = proxys.toArray(new Object[0]);
        //return CompletableFuture.completedFuture(proxyArray);
        return CompletableFuture.completedFuture(proxys.toArray());
    }
}
