
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {
  public static final RestTemplate restTemplate = new RestTemplate();
  public static final PortfolioManager portFolioManager = 
      PortfolioManagerFactory.getPortfolioManager("tiingo",restTemplate);

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    String filename = args[0];
    File inputFile = resolveFileFromResources(filename);
    ArrayList<String> listOfSymbols = new ArrayList<String>();

    ObjectMapper objMapper = getObjectMapper();

    PortfolioTrade[] trades = objMapper.readValue(inputFile, PortfolioTrade[].class);
    for (PortfolioTrade s : trades) {
      listOfSymbols.add(s.getSymbol());
    }
    return listOfSymbols;

  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    File filename = resolveFileFromResources(args[0]);
    String endDate = args[1];
    String token = "d37bb35d84e67aea889b436222856c0fbe1d1ff7";
    String url = "https://api.tiingo.com/tiingo/daily/";
    RestTemplate restTemplate = new RestTemplate();
    ArrayList<TotalReturnsDto> results = new ArrayList<>();
    TiingoCandle[] candles = null;

    ObjectMapper objMapper = getObjectMapper();
    PortfolioTrade[] trades = objMapper.readValue(filename, PortfolioTrade[].class);

    for (PortfolioTrade s : trades) {
      candles = restTemplate.getForObject(url + s.getSymbol() + "/prices?startDate=" 
      + s.getPurchaseDate().toString() + "&endDate=" 
      + endDate + "&token=" + token, TiingoCandle[].class);

      if (candles != null) {
        TotalReturnsDto totalReturns = new TotalReturnsDto(s.getSymbol(), 
            candles[candles.length - 1].getClose());
        results.add(totalReturns);
      }
    }

    Collections.sort(results, comp);
    ArrayList<String> outputStock = new ArrayList<String>();

    for (TotalReturnsDto r : results) {
      outputStock.add(r.getSymbol());
    }

    return outputStock;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) 
      throws IOException, URISyntaxException {

    File filename = resolveFileFromResources(args[0]);
    String endDate = args[1];
  
    ObjectMapper objMapper = getObjectMapper();
    PortfolioTrade[] trades = objMapper.readValue(filename, PortfolioTrade[].class);
    ArrayList<AnnualizedReturn> annualizedReturnsList = new ArrayList<>();

    for (PortfolioTrade trade : trades) {
      annualizedReturnsList.add(getAnnualizedReturn(trade, LocalDate.parse(endDate)));
    }

    Collections.sort(annualizedReturnsList, descComp);

    return annualizedReturnsList;
  }

  public static final Comparator<AnnualizedReturn> descComp = new Comparator<AnnualizedReturn>() {
    public int compare(AnnualizedReturn t1, AnnualizedReturn t2) {
      return (int) (t2.getAnnualizedReturn().compareTo(t1.getAnnualizedReturn()));
    }

  };

  public static AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endDate) {
    String token = "d37bb35d84e67aea889b436222856c0fbe1d1ff7";
    String url = "https://api.tiingo.com/tiingo/daily/";
    RestTemplate restTemplate = new RestTemplate();
    TiingoCandle[] candles = restTemplate.getForObject(url 
        + trade.getSymbol() + "/prices?startDate="
        + trade.getPurchaseDate().toString() + "&endDate=" 
        + endDate + "&token=" + token, TiingoCandle[].class);

    if (candles != null) {
      TiingoCandle first = candles[0];
      TiingoCandle last = candles[candles.length - 1];
      AnnualizedReturn annualReturn = calculateAnnualizedReturns(endDate, 
          trade, first.getOpen(), last.getClose());
      return annualReturn;
    }

    return new AnnualizedReturn(trade.getSymbol(), 0.0, 0.0);

  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, 
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double years = (double) ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365;
    double rfactor = 1 / years;
    double res = Math.pow(1 + totalReturns, rfactor) - 1;
    return new AnnualizedReturn(trade.getSymbol(), res, totalReturns);
  }

  public static final Comparator<TotalReturnsDto> comp = new Comparator<TotalReturnsDto>() {

    public int compare(TotalReturnsDto t1, TotalReturnsDto t2) {
      return (int) (t1.getClosingPrice().compareTo(t2.getClosingPrice()));
    }

  };
  
  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader()
    .getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    File contents = resolveFileFromResources(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] trades = objectMapper.readValue(contents, PortfolioTrade[].class);

    return portFolioManager.calculateAnnualizedReturn(Arrays.asList(trades), endDate);
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = 
        "File@51 /home/crio-user/workspace/sahaankita450-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@4988d8b8";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, 
      toStringOfObjectMapper,functionNameFromTestFileInStackTrace, 
      lineNumberFromTestFileInStackTrace });
  }



  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());




  }
}

