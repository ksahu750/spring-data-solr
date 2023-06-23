package org.springframework.data.solr.test.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.solr.api.V2HttpCall;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.CommandOperation;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.servlet.HttpSolrCall;
import org.apache.solr.servlet.SolrRequestParsers;
import org.apache.solr.util.RTimerTree;

import java.io.File;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SolrRequestParsersExtended extends SolrRequestParsers {

    private final boolean enableRemoteStreams;
    private final boolean enableStreamBody;

    /**
     * Pass in an xml configuration. A null configuration will enable everything with maximum values.
     *
     * @param globalConfig
     */
    public SolrRequestParsersExtended(SolrConfig globalConfig) {
        super(globalConfig);
        if (globalConfig == null) {
            this.enableRemoteStreams = false;
            this.enableStreamBody = false;
        } else {
            this.enableRemoteStreams = globalConfig.isEnableRemoteStreams();
            this.enableStreamBody = globalConfig.isEnableStreamBody();
        }
    }

    public SolrQueryRequest buildRequestFrom(SolrCore core, SolrParams params, Collection<ContentStream> streams,
                                             RTimerTree requestTimer, final HttpServletRequest req) throws Exception {
        // The content type will be applied to all streaming content
        String contentType = params.get(CommonParams.STREAM_CONTENTTYPE);

        // Handle anything with a remoteURL
        String[] strs = params.getParams(CommonParams.STREAM_URL);
        if (strs != null) {
            if (!enableRemoteStreams) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Remote Streaming is disabled.");
            }
            for (final String url : strs) {
                ContentStreamBase stream = new ContentStreamBase.URLStream(new URL(url));
                if (contentType != null) {
                    stream.setContentType(contentType);
                }
                streams.add(stream);
            }
        }

        // Handle streaming files
        strs = params.getParams(CommonParams.STREAM_FILE);
        if (strs != null) {
            if (!enableRemoteStreams) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Remote Streaming is disabled. See http://lucene.apache.org/solr/guide/requestdispatcher-in-solrconfig.html for help");
            }
            for (final String file : strs) {
                ContentStreamBase stream = new ContentStreamBase.FileStream(new File(file));
                if (contentType != null) {
                    stream.setContentType(contentType);
                }
                streams.add(stream);
            }
        }

        // Check for streams in the request parameters
        strs = params.getParams(CommonParams.STREAM_BODY);
        if (strs != null) {
            if (!enableStreamBody) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Stream Body is disabled. See http://lucene.apache.org/solr/guide/requestdispatcher-in-solrconfig.html for help");
            }
            for (final String body : strs) {
                ContentStreamBase stream = new ContentStreamBase.StringStream(body);
                if (contentType != null) {
                    stream.setContentType(contentType);
                }
                streams.add(stream);
            }
        }

        final HttpSolrCall httpSolrCall = req == null ? null : (HttpSolrCall) req.getAttribute(HttpSolrCall.class.getName());
        SolrQueryRequestBase q = new SolrQueryRequestBase(core, params, requestTimer) {
            @Override
            public Principal getUserPrincipal() {
                return req == null ? null : req.getUserPrincipal();
            }

            @Override
            public List<CommandOperation> getCommands(boolean validateInput) {
                if (httpSolrCall != null) {
                    return httpSolrCall.getCommands(validateInput);
                }
                return super.getCommands(validateInput);
            }

            @Override
            public Map<String, String> getPathTemplateValues() {
                if (httpSolrCall != null && httpSolrCall instanceof V2HttpCall) {
                    return ((V2HttpCall) httpSolrCall).getUrlParts();
                }
                return super.getPathTemplateValues();
            }

            @Override
            public HttpSolrCall getHttpSolrCall() {
                return httpSolrCall;
            }
        };
        if (streams != null && streams.size() > 0) {
            q.setContentStreams(streams);
        }
        return q;
    }

}
