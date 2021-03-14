
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private StockQuotesService stockService;
  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  public PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockService = stockQuotesService;
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {

    return stockService.getStockQuote(symbol, from, to);

  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> 
      portfolioTrades, LocalDate endDate) throws StockQuoteServiceException {

    ArrayList<AnnualizedReturn> annualizedReturnsList = new ArrayList<AnnualizedReturn>();

    for (PortfolioTrade trade : portfolioTrades) {
      annualizedReturnsList.add(getAnnualizedReturn(trade, endDate));
    }

    Collections.sort(annualizedReturnsList, descComp);

    return annualizedReturnsList;

  }

  public static final Comparator<AnnualizedReturn> descComp = new Comparator<AnnualizedReturn>() {
    public int compare(AnnualizedReturn t1, AnnualizedReturn t2) {
      return (int) (t2.getAnnualizedReturn().compareTo(t1.getAnnualizedReturn()));
    }

  };

  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endDate)
      throws StockQuoteServiceException {
    AnnualizedReturn annualizedReturn;
    try {
      List<Candle> stocks;
      stocks = this.getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      Candle stockFirst = stocks.get(0);
      Candle stockLast = stocks.get(stocks.size() - 1);
      Double buyPrice = stockFirst.getOpen();
      Double sellPrice = stockLast.getClose();
      double totalReturns = (sellPrice - buyPrice) / buyPrice;
      double years = (double) ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365;
      double rfactor = 1 / years;
      double res = Math.pow(1 + totalReturns, rfactor) - 1;
      annualizedReturn = new AnnualizedReturn(trade.getSymbol(), res, totalReturns);
    } catch (JsonProcessingException e) {
      annualizedReturn = new AnnualizedReturn(trade.getSymbol(), 0.0, 0.0);
      return annualizedReturn;
    }
    return annualizedReturn;

  }

  public List<Candle> getStockQuotes(String symbol, LocalDate from, LocalDate to) 
        throws JsonProcessingException {

    String url = buildUri(symbol, from, to);
    TiingoCandle[] stocks = this.restTemplate.getForObject(url, TiingoCandle[].class);
    if (stocks == null) {
      return new ArrayList<Candle>();
    }

    return Arrays.asList(stocks);

  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String url = "https://api.tiingo.com/tiingo/daily/";
    String token = "d37bb35d84e67aea889b436222856c0fbe1d1ff7";

    String uri = url + symbol + "/prices?startDate=" + startDate.toString() 
        + "&endDate=" + endDate.toString() + "&token=" + token;

    return uri;
  }

  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, 
      int numThreads) throws InterruptedException, StockQuoteServiceException {

    ArrayList<AnnualizedReturn> annualizedReturnsList = new ArrayList<AnnualizedReturn>();
    int numLength = 20;
    ExecutorService executor = Executors.newFixedThreadPool(numLength);
    ArrayList<Future<AnnualizedReturn>> futureResults = new ArrayList<Future<AnnualizedReturn>>();

    for (PortfolioTrade trade : portfolioTrades) {

      Callable<AnnualizedReturn> callableTask = () -> {
        return getAnnualizedReturn(trade, endDate);

      };

      Future<AnnualizedReturn> resultList = executor.submit(callableTask);
      futureResults.add(resultList);

    }

    AnnualizedReturn annualReturn = null;;
    for (int i = 0; i < portfolioTrades.size(); i++) {

      Future<AnnualizedReturn> futureResult = futureResults.get(i);
      
      try {
        annualReturn = futureResult.get();
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException("Exception while calling API");
      }
      annualizedReturnsList.add(annualReturn);

    }

    Collections.sort(annualizedReturnsList, descComp);

    return annualizedReturnsList;





  }

  

}
