package space.qyvlik.fiat.exchange.rates.modules.provider.oneforge;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import space.qyvlik.fiat.exchange.rates.modules.provider.AbstractFiatExchangeRatesProvider;
import space.qyvlik.fiat.exchange.rates.modules.provider.entity.request.Account;
import space.qyvlik.fiat.exchange.rates.modules.provider.entity.result.FiatExchangeRate;

import java.util.List;

@Service
public class OneForgeProvider extends AbstractFiatExchangeRatesProvider {

    @Autowired
    private RestTemplate restTemplate;

    private String host = "api.1forge.com";

    public OneForgeProvider() {
        super("OneForge");
    }

    @Override
    public List<FiatExchangeRate> findExchangeRateList(Account account) {

        if (StringUtils.isBlank(account.getAccessKey())) {
            throw new RuntimeException("provider:" + provider + ", accesskey is empty");
        }

        if (StringUtils.isBlank(account.getPlan())) {
            throw new RuntimeException("provider:" + provider + ", plan is empty");
        }

        List<FiatExchangeRate> list = Lists.newArrayList();

        final String path = "/quotes?api_key=" + account.getAccessKey();

        String url = "https://" + host + path;

        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            String body = response.getBody();
            if (StringUtils.isBlank(body)) {
                logger.error("findExchangeRateList fail : error: body is empty");
                return list;
            }

            if (body.startsWith("{")) {
                logger.error("findExchangeRateList fail : error:" + body);
                return list;
            }

            List<OneForgeQuote> quoteList = JSON.parseArray(body).toJavaList(OneForgeQuote.class);

            if (quoteList == null || quoteList.size() == 0) {
                logger.error("findExchangeRateList fail : error: quoteList is empty");
                return list;
            }

            for (OneForgeQuote quote : quoteList) {
                String pair = quote.getSymbol();

                String baseCurrency = pair.substring(0, 3);
                String quoteCurrency = pair.substring(3, 6);

                FiatExchangeRate fiatRate = new FiatExchangeRate();

                fiatRate.setProvider(getProvider());
                fiatRate.setQuote(quoteCurrency);
                fiatRate.setBase(baseCurrency);
                fiatRate.setTs(quote.getTimestamp() * 1000);
                fiatRate.setRate(quote.getPrice());

                list.add(fiatRate);
            }

        } catch (Exception e) {
            logger.error("findExchangeRateList fail : error:{}", e.getMessage());
        }

        return list;
    }
}
