package com.eaishipment.failureanalysis.analyzer;

public interface FailureAnalyzer {
    String getName();
    String analyze(FailureAnalysisContext context);
}
