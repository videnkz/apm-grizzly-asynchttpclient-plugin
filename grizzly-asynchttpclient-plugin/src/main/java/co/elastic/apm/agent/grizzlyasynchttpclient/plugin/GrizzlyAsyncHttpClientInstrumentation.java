package co.elastic.apm.agent.grizzlyasynchttpclient.plugin;

import co.elastic.apm.agent.sdk.ElasticApmInstrumentation;
import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Request;
import com.ning.http.client.uri.Uri;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class GrizzlyAsyncHttpClientInstrumentation extends ElasticApmInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named("com.ning.http.client.AsyncHttpClient");
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("executeRequest")
                .and(takesArgument(0, named("com.ning.http.client.Request"))
                        .and(takesArgument(1, named("com.ning.http.client.AsyncHandler"))));
    }

    @Override
    public String getAdviceClassName() {
        return getClass().getName() + "$GrizzlyAsyncHttpClientAdvice";
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return Arrays.asList("http-client", "grizzly-async-httpclient");
    }

    public static class GrizzlyAsyncHttpClientAdvice {
        @Nullable
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(index = 0, value = 1, typing = DYNAMIC))
        public static Object[] onBeforeSend(@Advice.Argument(0) Request request,
                                            @Advice.Argument(1) AsyncHandler<?> asyncHandler) {
            Transaction parent = ElasticApm.currentTransaction();
            if (parent.getId().isEmpty() || request == null) {
                return new Object[]{asyncHandler, null};
            }
            Span ret = parent.startExitSpan("external", "http", "");
            Uri uri = request.getUri();
            String host = uri.getHost();
            int port = definePort(uri);
            ret = ret.setName(request.getMethod() + " " + host);
            ret = ret.setDestinationAddress(host, port);
            ret.injectTraceHeaders((headerName, headerValue) -> request.getHeaders().put(headerName, Collections.singletonList(headerValue)));
            ret.activate();
            return new Object[]{new AsyncHandlerWrapper<>(asyncHandler, ret), ret};
        }

        private static int definePort(final Uri uri) {
            int port = uri.getPort();
            if (-1 != port) {
                return port;
            }
            String scheme = uri.getScheme();
            if (null == scheme) {
                return -1;
            }
            if ("http".equals(scheme)) {
                port = 80;
            } else if ("https".equals(scheme)) {
                port = 443;
            }
            return port;
        }

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
        public static void onAfterSend(@Advice.Thrown Throwable thrown,
                                       @Advice.Enter @Nullable Object[] entryArgs) {
            if (entryArgs == null) {
                return;
            }
            if (entryArgs[1] instanceof Span && thrown != null) {
                Span span = (Span) entryArgs[1];
                span.captureException(thrown);
            }
        }
    }
}
