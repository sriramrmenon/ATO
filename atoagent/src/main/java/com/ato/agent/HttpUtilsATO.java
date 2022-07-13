package com.ato.agent;


//import jakarta.servlet.http.HttpServletRequest;

import com.ato.agent.dto.ClientConfiguration;
import com.ato.agent.dto.ReportDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.apache.commons.lang3.SerializationUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
public class HttpUtilsATO {


    static OkHttpClient okhttp =
            new OkHttpClient.Builder()
                    .connectTimeout(3000, TimeUnit.MILLISECONDS)
                    .readTimeout(3000, TimeUnit.MILLISECONDS)
                    .writeTimeout(3000, TimeUnit.MILLISECONDS)
                    .build();


    /**
     *
     * @param request
     */
    public void analyzeRequest(HttpServletRequest request,Object requestBody){

//        ServletRequest requestWrapper = null;
////        if (request instanceof HttpServletRequest) {
////            try {
////                requestWrapper = new AtoHttpServletRequestWrapper((HttpServletRequest) request);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////        }

        Map<String, String> result=getRequestHeadersInMap(request);
        final Gson gson = new GsonBuilder().create();
        ReportDTO report=new ReportDTO();
        report.setIpaddress(getIPAddress(request));
        report.setDescription(" Number of Headers detected in the request is more than expected ");
        report.setDatectedData(gson.toJson(result));
        report.setAppId(ClientConfiguration.config.getAppId());
        report.setDate(String.valueOf(new Date()));
        String jsonBody=gson.toJson(report);
        if(result.size()>10){
            apiCall(jsonBody);
        }
        verifyRequestSize(request,requestBody);
    }


    /**
     *
     * @param request
     */
    public void verifyRequestSize(HttpServletRequest request,Object object){

        final Gson gson = new GsonBuilder().create();
//                byte[] utf16Bytes = SerializationUtils.serialize((Serializable) object);
//                final byte[] utf16Bytes= requestData.getBytes("UTF-16BE");

                final byte[] utf16Bytes;


        try {
            String jsonData= gson.toJson(object);
            utf16Bytes= jsonData.getBytes("UTF-16BE");
            int byteLength=utf16Bytes.length;
            System.out.println(byteLength);
            if(25000 < byteLength){
                ReportDTO report=new ReportDTO();
                report.setIpaddress(getIPAddress(request));
                report.setDescription(" Request body exceeded the maximum size ");
                report.setDatectedData("byteLength = "+byteLength );
                report.setAppId(ClientConfiguration.config.getAppId());
                report.setDate(String.valueOf(new Date()));
                String jsonBody=gson.toJson(report);
                apiCall(jsonBody);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }



//        }
    }


    /**
     *
     * @param request
     * @return
     */
     public Map<String, String> getRequestHeadersInMap(HttpServletRequest request) {

        Map<String, String> result = new HashMap<>();
        Enumeration headerNames = request.getHeaderNames();
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, String> headers = Collections.list(httpRequest.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, httpRequest::getHeader));

        int headerSize=headers.size();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            result.put(key, value);
        }

        getIPAddress(request);
        return result;
    }

    public String getIPAddress(HttpServletRequest request){
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    /**
     *
     */
    public void apiCall(String jsonBody) {
        System.out.println(" Calling the API for reporting ajshgdjasgdjashdgjasgd **8 ---"+jsonBody);
        try {
            String apiUrl = "http://3.8.16.176:8080/ato-endpoints/reports";
            System.out.println(" Connecting to the the endpoint, "+apiUrl);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            Request request = new Request.Builder()
//                    .url("http://localhost:8080/reports")
                    .url(apiUrl)
                    .header("User-Agent", "OkHttp Headers.java")
                    .addHeader("Accept", "application/json; q=0.5")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("appid",ClientConfiguration.config.getAppId())
                    .post(body)
                    .build();

            Call call = okhttp.newCall(request);
            Response response = call.execute();
            System.out.println(response.body().string());

        } catch (Exception e) {

        }
    }

}
