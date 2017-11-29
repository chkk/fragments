package appointment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
/**
 * 健身房预约
 * @author chenkai6
 *
 */
public class Appointment {
	
	private static BasicCookieStore cookieStore = new BasicCookieStore();
	private static String username = "";
	private static String password = "";
	private static String [] appInfoIdArray = null;
	private static String executeTime = "";
	private static String picId;
	private static String cjyusername = "chen001";
	private static String cjypassword = "chen123";
	private static String softId = "892717";
	private static String filePath = "E:\\temp233\\validate_code.png";
	private static String executePath = "E:\\temp233";
	private static Boolean executeFlag = true;
	
	public static void main(String[] args) throws InterruptedException {
		 Properties prop = new Properties();
		 try {
			InputStream in = new BufferedInputStream(new FileInputStream(executePath + "\\config.properties"));
			prop.load(in);
			username = (String) prop.getProperty("username");
			password = (String) prop.getProperty("password");
			String appInfoIds = prop.getProperty("appInfoIds");
			appInfoIdArray = appInfoIds.split(",");
			executeTime = (String) prop.getProperty("executeTime");
		} catch (Exception e) {
			System.err.println("未读取到正确配置");
			executeFlag = false;
			Thread.sleep(3000000);
			System.exit(0);
		}
		DateFormat df = DateFormat.getTimeInstance();
		while(executeFlag){
			if(isInTime(executeTime, df.format(new Date()))){
				//模拟登录，获取cookie
				String loginUrl = "http://ssa.jd.com/sso/login";
		        List<NameValuePair> loginFormParams = new ArrayList<NameValuePair>();  
		        loginFormParams.add(new BasicNameValuePair("username", username));  
		        loginFormParams.add(new BasicNameValuePair("password", password));  
				postForm(loginUrl,loginFormParams);
				List<String> appInfoIdList = new ArrayList<String>();
				Collections.addAll(appInfoIdList, appInfoIdArray);
				int i;
				for (i = 0; i < appInfoIdList.size(); i++) {
					while(executeFlag){
						String appointmentStatus = task(appInfoIdList.get(i));
						Gson gson = new Gson();
						Map<String, String> infoMap = gson.fromJson(appointmentStatus, new TypeToken<Map<String, String>>(){}.getType());
						String message = (String) infoMap.get("message");
						System.out.println("预约 "+appInfoIdList.get(i)+"服务器状态:"+message);
						if(message.equals("您已经预约，请不要重复提交")){
							System.out.println("预约成功，程序暂停");
							executeFlag = false;
							Thread.sleep(3000000);
							System.exit(0);
						}
						if(message.equals("预约成功")){
							System.out.println("预约成功，程序暂停");
							executeFlag = false;
							Thread.sleep(3000000);
							System.exit(0);
						}
						if(message.equals("当前预约人数已满")){
							System.out.println("当前健身房人数已满，即将预约下一个");
							break;
						} 
						if(message.equals("验证码错误")){
							System.out.println("验证码错误，即将重新验证");
//							ChaoJiYing.ReportError(cjyusername,cjypassword,softId,picId);
							continue;
						}
							break;
					}
					if(i == appInfoIdList.size() -1){
						System.out.println("已遍历完所有健身房,程序暂停");
						executeFlag = false;
						Thread.sleep(3000000);
						System.exit(0);
					}
				}
			}else{
				System.out.println("还没有到预约时间哦~");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String task(String appInfoId){
		//获得验证码
			long time = new Date().getTime();
			String validateUrl = "http://ssc.jd.com/reservation/index/validate_code?yys="+Long.toString(time);
			getValidateCode(validateUrl);
			String postPic = ChaoJiYing.PostPic(cjyusername,cjypassword,softId,"1104","0","0","0",filePath);
			Gson gson = new Gson();
			Map<String, String> infoMap = gson.fromJson(postPic, new TypeToken<Map<String, String>>(){}.getType());
			String picStr = (String) infoMap.get("pic_str");
			picId = (String) infoMap.get("pic_id");
//			预约
			String AppointmentUrl = "http://ssc.jd.com/reservation/index/booking_confirm";
//			{appStepId:appStepId,categoryId:categoryId,appInfoId:appInfoId,code:validateCode}
			String appStepId = "116";
			String categoryId = "17";
				
			List<NameValuePair> appointmentFormParams = new ArrayList<NameValuePair>();
			
			appointmentFormParams.add(new BasicNameValuePair("appStepId", appStepId));
			appointmentFormParams.add(new BasicNameValuePair("categoryId", categoryId));
			appointmentFormParams.add(new BasicNameValuePair("appInfoId", appInfoId));
			appointmentFormParams.add(new BasicNameValuePair("code", picStr));
			return postForm(AppointmentUrl,appointmentFormParams);
	}
	
    public static String postForm(String url,List<NameValuePair> formparams) {  
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();

        HttpPost httppost = new HttpPost(url);  
        UrlEncodedFormEntity uefEntity;  
        try {  
            uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");  
            httppost.setEntity(uefEntity);  
            CloseableHttpResponse response = httpclient.execute(httppost);  
            try {  
            	 HttpEntity entity = response.getEntity();  
                 if (entity != null) {  
                	return EntityUtils.toString(entity, "UTF-8");
                 }  
            } finally {  
                response.close();  
            }  
        } catch (ClientProtocolException e) {  
            e.printStackTrace();  
        } catch (UnsupportedEncodingException e1) {  
            e1.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                httpclient.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }
		return null;  
    }
    
    public static void getValidateCode(String validateUrl) {
    	CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
		try {
			HttpGet httpget = new HttpGet(validateUrl);
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				HttpEntity entity = response.getEntity();
		         if (entity != null) {  
                	 InputStream input = entity.getContent();
                	 File file =new File(executePath);
                	 if (!file .exists()  && !file .isDirectory())      
                	 {       
                	     file .mkdir();    
                	 }
                     OutputStream output = new FileOutputStream(new File(executePath + "\\validate_code.png"));
                     IOUtils.copy(input, output);
                     output.flush();
		         	}
			} finally {
				response.close();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
    
    public static boolean isInTime(String sourceTime, String curTime) {
        if (sourceTime == null || !sourceTime.contains("-") || !sourceTime.contains(":")) {
            throw new IllegalArgumentException("Illegal Argument arg:" + sourceTime);
        }
        if (curTime == null || !curTime.contains(":")) {
            throw new IllegalArgumentException("Illegal Argument arg:" + curTime);
        }
        String[] args = sourceTime.split("-");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        try {
            long now = sdf.parse(curTime).getTime();
            long start = sdf.parse(args[0]).getTime();
            long end = sdf.parse(args[1]).getTime();
            if (args[1].equals("00:00:00")) {
                args[1] = "24:00:00";
            }
            if (end < start) {
                if (now >= end && now < start) {
                    return false;
                } else {
                    return true;
                }
            } 
            else {
                if (now >= start && now < end) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Illegal Argument arg:" + sourceTime);
        }
    }
}