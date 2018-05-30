package com.skycloud.auth.client.interceptor;

import com.skycloud.auth.client.client.AuthClient;
import com.skycloud.auth.client.configuration.ClientConfiguration;
import com.skycloud.auth.client.configuration.UserAuthConfiguration;
import com.skycloud.common.base.BaseContextHandler;
import com.skycloud.common.base.Result;
import com.skycloud.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Objects;

import static java.util.Optional.ofNullable;


/**
 * @author ace
 */
@Component
@Slf4j
public class OkHttpTokenInterceptor implements Interceptor {

    @Resource
    private AuthClient authClient;

    @Resource
    private ClientConfiguration clientConfiguration;

    @Resource
    private UserAuthConfiguration userAuthConfiguration;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request newRequest = null;
        String url = Objects.toString(chain.request().url());
        if (url.contains("client/token") || url.contains("/client/myClient")) {
            newRequest = chain.request()
                    .newBuilder()
                    .header(userAuthConfiguration.getUserTokenHeader(), BaseContextHandler.getToken())
                    .build();
        } else {
            Result<String> token = authClient.getAccessToken(clientConfiguration.getClientId(), clientConfiguration.getSecret());
            String data = ofNullable(token).orElse(new Result<>()).getData();
            newRequest = chain.request()
                    .newBuilder()
                    .header(userAuthConfiguration.getUserTokenHeader(), BaseContextHandler.getToken())
                    .header(clientConfiguration.getClientTokenHeader(), data)
                    .build();
        }
        Response response = chain.proceed(newRequest);
        if (HttpStatus.FORBIDDEN.value() == response.code()) {
            if (response.body().string().contains(String.valueOf(CommonConstants.EX_CLIENT_INVALID_CODE))) {
                log.info("Client Token Expire,Retry to request...");
//                authClient.refreshClientToken();
                Result<String> token = authClient.getAccessToken(clientConfiguration.getClientId(), clientConfiguration.getSecret());
                String data = ofNullable(token).orElse(new Result<>()).getData();
                newRequest = chain.request()
                        .newBuilder()
                        .header(userAuthConfiguration.getUserTokenHeader(), BaseContextHandler.getToken())
                        .header(clientConfiguration.getClientTokenHeader(), data)
                        .build();
                response = chain.proceed(newRequest);
            }
        }
        return response;
    }
}
