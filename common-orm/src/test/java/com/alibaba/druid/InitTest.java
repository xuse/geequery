package com.alibaba.druid;

import java.io.File;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import jef.database.DbClient;
import jef.database.DbClientBuilder;

public class InitTest {

	@Test
	public void buildTest() {
		DbClient db = new DbClientBuilder("derby", new File("./db"), null, null).build();
	}

	@Test
	public void testJson() {
		String json = "{\"aaaa\":123,\"bbb\":\"sss\"}";
		JSON jo = JSON.parseObject(json, JSONObject.class);
		System.out.println("[" + jo + "]");
	}
}
