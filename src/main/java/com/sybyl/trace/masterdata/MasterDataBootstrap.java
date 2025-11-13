package com.sybyl.trace.masterdata;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MasterDataBootstrap {
  private final AdditionalExpenseLabelService labels;
  @PostConstruct
  public void init() {
    labels.ensureDefault();
  }
}
