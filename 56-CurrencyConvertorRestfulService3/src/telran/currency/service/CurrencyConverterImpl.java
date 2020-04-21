package telran.currency.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import telran.currency.dto.CurrencyRates;
import telran.currency.interfaces.ICurrencyConverter;

@Service
@ManagedResource
public class CurrencyConverterImpl implements ICurrencyConverter {

	static private final String url = "http://data.fixer.io/api/latest?access_key=53ce63adc508e94144574ea8c794aaf8";
	private static final int CODES_PER_LINE = 20;
	private CurrencyRates currencyRates;
	long requestPeriodStart;

	@Value("${refreshPeriod:60}")
	int minutes;
	private int convertRequests = 0;

	@ManagedAttribute
	public int getMinutes() {
		return minutes;
	}

	@ManagedAttribute
	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	public CurrencyConverterImpl() {
		currencyRates = createCurrencyRates();
	}

	private CurrencyRates createCurrencyRates() {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<CurrencyRates> responce = restTemplate.exchange(url, HttpMethod.GET, null, CurrencyRates.class);
		this.requestPeriodStart = System.currentTimeMillis();
		return responce.getBody();
	}

	@Override
	public String lastDateTimePresentation() {
		if (checkRequestPeriod(requestPeriodStart)) {
			currencyRates = createCurrencyRates();
		}
		Instant date = Instant.ofEpochSecond(currencyRates.timestamp);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
		LocalDateTime localDate = LocalDateTime.ofInstant(date, ZoneId.of("Asia/Jerusalem"));
		return localDate.format(formatter);
	}

	@Override
	public Set<String> getCodes() {
		if (checkRequestPeriod(requestPeriodStart)) {
			currencyRates = createCurrencyRates();
		}
		return currencyRates.rates.keySet();

	}

	private boolean checkRequestPeriod(long requestPeriodStart) {
		if ((System.currentTimeMillis() - requestPeriodStart) / 1000 / 60 >= minutes) {
			return true;
		}
		return false;
	}

	@Override
	public double convert(String from, String to, double amount) {
		if (checkRequestPeriod(requestPeriodStart)) {
			currencyRates = createCurrencyRates();
		}
		if (from == null || to == null) {
			throw new IllegalArgumentException();
		}
		Map<String, Double> rates = currencyRates.rates;
		
		convertRequests ++;
		return amount / rates.get(from) * rates.get(to);
	}

	@Override
	public String displayRefreshPeriodTime() {
		return String.valueOf(getMinutes()) + " minutes";
	}

	@PostConstruct
	void showAllCurrenciesCodes() {
		//now copy-paste code is coming  ;)
		Set<String> codes = getCodes();		
		int counter = 0;
		StringBuilder line = new StringBuilder();
		for (String code : codes) {
			line.append(code).append(" ");
			if (++counter % CODES_PER_LINE == 0) {
				System.out.println(line.toString());
				line.setLength(0);			
			}
		}
		if(line.length() > 0) {
			System.out.println(line.toString());
		}
	}
	
	@PreDestroy
	void amountOfTheProcessedConvertRequests() {
		System.out.println("Amount of the processed “convert” requests is: "+convertRequests);
	}

	@Override
	public double euroValue(String code) {
		if (checkRequestPeriod(requestPeriodStart)) {
			currencyRates = createCurrencyRates();
		}
		if (!getCodes().contains(code.toUpperCase())) {
			return -1;
		}else {
			return currencyRates.rates.get(code.toUpperCase());
		}
	}	
}
