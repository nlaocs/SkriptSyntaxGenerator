package jp.nlaocs.skriptSyntaxGenerator.hook;

import java.lang.instrument.Instrumentation;

public interface Hook {
    String name();

    void install(Instrumentation instrumentation) throws Exception;
}

