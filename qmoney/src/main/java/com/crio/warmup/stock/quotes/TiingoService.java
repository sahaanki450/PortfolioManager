
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {


  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
         throws JsonProcessingException, StockQuoteServiceException {
    TiingoCandle[] stocks = null;
    String url = buildUri(symbol, from, to);

    try {
      String stockQuotes = restTemplate.getForObject(url, String.class);

      ObjectMapper mapper = getObjectMapper();
      stocks = mapper.readValue(stockQuotes, TiingoCandle[].class);
    
      if (stocks == null) {
        return new ArrayList<Candle>();
      }
    } catch (NullPointerException e) {
      throw new StockQuoteServiceException("not available");
    } 
    return Arrays.asList(stocks);

  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String url = "https://api.tiingo.com/tiingo/daily/";
    String token = "d37bb35d84e67aea889b436222856c0fbe1d1ff7";
    
    String uri = url + symbol  + "/prices?startDate="
        + startDate.toString() 
        + "&endDate=" + endDate.toString() + "&token=" + token;

    return uri;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

}
