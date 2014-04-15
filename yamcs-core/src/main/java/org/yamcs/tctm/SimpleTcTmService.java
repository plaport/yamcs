package org.yamcs.tctm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.yamcs.ParameterProvider;

import com.google.common.util.concurrent.AbstractService;

public class SimpleTcTmService extends AbstractService implements TcTmService {
    TmPacketProvider tm;
    ParameterProvider param;
    TcUplinker tc;
    
    public SimpleTcTmService(TmPacketProvider tm, ParameterProvider param, TcUplinker tc) {
        this.tm=tm;
        this.param=param;
        this.tc=tc;
    }
    @Override
    public TmPacketProvider getTmPacketProvider() {
       return tm;
    }
    @Override
    public TcUplinker getTcUplinker() {
        return tc;
    }
    @Override
    public List<ParameterProvider> getParameterProviders() {
        if(param!=null) {
            return Arrays.asList(param);
        } else {
            return new ArrayList<ParameterProvider>();
        }
    }
    @Override
    protected void doStart() {
        if(tm!=null) tm.start();
        if(param!=null) param.start();
    }
    @Override
    protected void doStop() {
        if(tm!=null) tm.stop();
        if(param!=null) param.stop();
    }
}
