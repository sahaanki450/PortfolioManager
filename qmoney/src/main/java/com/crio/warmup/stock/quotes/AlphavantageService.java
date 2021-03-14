
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  
  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException, StockQuoteServiceException {
    String url = buildUri(symbol);
    List<Candle> stocks = null;

    try {
      String candlesResponse = 
          this.restTemplate.getForObject(url, String.class);
    
      ObjectMapper mapper = getObjectMapper();
      Map<LocalDate, AlphavantageCandle> candles =  
          mapper.readValue(candlesResponse,AlphavantageDailyResponse.class).getCandles(); 

      stocks = new ArrayList<>();
      for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
        AlphavantageCandle alphavantageCandle = candles.get(date);
        if (alphavantageCandle != null) {
          alphavantageCandle.setDate(date);
          stocks.add(alphavantageCandle);
        }
      }
    } catch (NullPointerException e) {
      throw new StockQuoteServiceException("No data availabale");
    }  
  
    return stocks;
  }

  private String buildUri(String symbol) {

    String key = "LM6QHRLEVP4F1H78";
    String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + symbol + "&apikey=" + key;
    return url;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}

