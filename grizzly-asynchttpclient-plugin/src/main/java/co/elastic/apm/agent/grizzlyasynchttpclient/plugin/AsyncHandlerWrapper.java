package co.elastic.apm.agent.grizzlyasynchttpclient.plugin;

import co.elastic.apm.api.Outcome;
import co.elastic.apm.api.Span;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

public class AsyncHandlerWrapper<T> implements AsyncHandler<T> {

    private final AsyncHandler<T> delegate;
    private final Span span;

    public AsyncHandlerWrapper(AsyncHandler<T> delegate, Span span) {
        this.delegate = delegate;
        this.span = span;
    }

    @Override
    public void onThrowable(Throwable throwable) {
        try {
            delegate.onThrowable(throwable);
            if (null == span) {
                return;
            }
            span.setOutcome(Outcome.FAILURE);
            span.captureException(throwable);
        } finally {
            if (null != span) {
                span.end();
            }
        }
    }

    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart httpResponseBodyPart) throws Exception {
        return delegate.onBodyPartReceived(httpResponseBodyPart);
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus httpResponseStatus) throws Exception {
        return delegate.onStatusReceived(httpResponseStatus);
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders httpResponseHeaders) throws Exception {
        return delegate.onHeadersReceived(httpResponseHeaders);
    }

    @Override
    public T onCompleted() throws Exception {
        try {
            T res = (T) delegate.onCompleted();
            if (null != span) {
                span.setOutcome(Outcome.SUCCESS);
            }
            return res;
        } finally {
            if (null != span) {
                span.end();
            }
        }
    }
}
