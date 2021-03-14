package com.crio.warmup.stock.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphavantageCandle implements Candle {
  @JsonProperty("1. open")
  private Double open;
  @JsonProperty("4. close")
  private Double close;
  @JsonProperty("2. high")
  private Double high;
  @JsonProperty("3. low")
  private Double low;

  private LocalDate date;

  @Override
  public Double getOpen() {
    return this.open;
  }

  @Override
  public Double getClose() {
    return this.close;
  }

  @Override
  public Double getHigh() {
    return this.high;
  }

  @Override
  public Double getLow() {
    return this.low;
  }

  @Override
  public LocalDate getDate() {
    return date;
  }

  public void setOpen(Double open) {
    this.open = open;
  }

  public void setClose(Double close) {
    this.close = close;
  }

  public void setHigh(Double high) {
    this.high = high;
  }

  public void setLow(Double low) {
    this.low = low;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  
}
