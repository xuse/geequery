/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.ORMConfig;
import jef.database.ConnectInfo;
import jef.database.DbCfg;
import jef.database.DebugUtil;
import jef.database.OperateTarget;
import jef.database.datasource.DataSourceInfo;
import jef.database.datasource.SimpleDataSource;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Double;
import jef.database.dialect.ColumnType.Int;
import jef.database.dialect.ColumnType.TimeStamp;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.EmuCoalesce_Nvl;
import jef.database.query.function.EmuOracleCast;
import jef.database.query.function.EmuOracleDateAdd;
import jef.database.query.function.EmuOracleDateSub;
import jef.database.query.function.EmuOracleExtract;
import jef.database.query.function.EmuOracleTime;
import jef.database.query.function.EmuOracleTimeStampDiff;
import jef.database.query.function.EmuOracleTimestampAdd;
import jef.database.query.function.EmuOracleToDate;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.query.function.TransformFunction;
import jef.database.query.function.VarArgsSQLFunction;
import jef.database.support.RDBMS;
import jef.database.wrapper.populator.ResultSetTransformer;
import jef.jre5support.ProcessUtil;
import jef.tools.DateFormats;
import jef.tools.DateUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.string.JefStringReader;
import oracle.sql.TIMESTAMP;

/**
 * 修改列名Oracle：lter table bbb rename column nnnnn to hh int;
 */
public class OracleDialect extends DbmsProfile {
	public OracleDialect() {
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(
				Feature.AUTOINCREMENT_NEED_SEQUENCE, 
				Feature.USER_AS_SCHEMA, 
				Feature.REMARK_META_FETCH, 
				Feature.BRUKETS_FOR_ALTER_TABLE, 
				Feature.SUPPORT_CONCAT,
				Feature.SUPPORT_CONNECT_BY,
				Feature.DROP_CASCADE,
				Feature.SUPPORT_SEQUENCE));

		super.loadKeywords("oracle_keywords.properties");
		if (JefConfiguration.getBoolean(DbCfg.DB_ENABLE_ROWID, true)) {
			features.add(Feature.SELECT_ROW_NUM);
		}
		setProperty(DbProperty.ADD_COLUMN, "ADD");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY");
		setProperty(DbProperty.DROP_COLUMN, "DROP");
		setProperty(DbProperty.CHECK_SQL, "SELECT 1 FROM DUAL");
		setProperty(DbProperty.SEQUENCE_FETCH, "SELECT %s.NEXTVAL FROM DUAL");
		setProperty(DbProperty.SELECT_EXPRESSION, "SELECT %s FROM DUAL");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"");
		
		registerNative(Scientific.sinh);
		registerNative(Scientific.cosh);
		registerNative(Scientific.tanh);
		registerNative(Scientific.exp);
		registerNative(Scientific.ln);
		registerNative(new StandardSQLFunction("log"));//Oracle can assign a log param.
		registerNative(new StandardSQLFunction("stddev"));
		
		
		registerNative(new StandardSQLFunction("variance"));

		registerNative(Func.round);
		registerNative(Func.trunc);
		registerNative(Func.ceil);
		registerNative(Func.floor);

		registerNative(new StandardSQLFunction("chr"));
		registerNative(new StandardSQLFunction("initcap"));
		registerNative(Func.lower);
		registerNative(Func.upper);
		registerNative(Func.trim);
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Scientific.soundex);
		registerNative(new StandardSQLFunction("ascii"));
		registerNative(Func.add_months);
		registerAlias("lcase", "lower");
		registerAlias("ucase", "upper");
		

		// Oracle的to_char有两种含义，具体如何转化需要根据传入参数来确定，单参数下是类型转换，双参数下是日期格式化
		registerNative(new StandardSQLFunction("to_char"));
		registerNative(new StandardSQLFunction("to_number"));
		registerNative(new StandardSQLFunction("to_date"));
		registerAlias(Func.date,"trunc");
		registerCompatible(Func.time,new EmuOracleTime());
		
		registerNative(new NoArgSQLFunction("sysdate", false));
		registerAlias(Func.current_timestamp, "sysdate");
		registerAlias(Func.now,"sysdate");
		

		
		registerNative(new NoArgSQLFunction("systimestamp", false));
		registerCompatible(Func.current_time, new TemplateFunction("current_time", "(systimestamp-trunc(sysdate))"));
		
		registerNative(new StandardSQLFunction("last_day"));
		registerNative(new NoArgSQLFunction("uid", false));
		registerNative(new NoArgSQLFunction("user", false));

		registerNative(new NoArgSQLFunction("rowid", false));
		registerNative(new NoArgSQLFunction("rownum", false));
		registerNative(Func.length);
		registerNative(Func.lengthb);
		
		
		registerNative(new StandardSQLFunction("instr"));
		registerNative(new StandardSQLFunction("instrb"));
		registerNative(Func.lpad);
		registerNative(Func.replace);
		registerNative(Func.rpad);
		registerNative(new StandardSQLFunction("substr"));
		registerAlias(Func.substring, "substr");
		registerNative(new StandardSQLFunction("substrb"));
		registerNative(Func.translate);
		

		// Multi-param numeric dialect functions...
		registerNative(new StandardSQLFunction("atan2"));
		
		registerNative(Func.mod);
		registerNative(Func.nvl);
		registerNative(Func.nullif);
		registerNative(Func.decode);
		registerNative(new StandardSQLFunction("nvl2"));
		
		registerNative(Scientific.power);
		
		// Multi-param date dialect functions...
		registerNative(new StandardSQLFunction("add_months"));
		registerNative(new StandardSQLFunction("months_between"));
		registerNative(new StandardSQLFunction("next_day"));
		
		registerCompatible(Func.current_date, new NoArgSQLFunction("trunc(sysdate)", false));
		
		registerCompatible(Func.concat, new VarArgsSQLFunction("", "||", ""));
		registerCompatible(Func.coalesce, new EmuCoalesce_Nvl());
		registerCompatible(Func.locate, new TransformFunction("locate", "instr",new int[]{2,1}));// 用instr来模拟locate参数相反
		registerCompatible(Func.year, new EmuOracleExtract("year",false));
		registerCompatible(Func.month, new EmuOracleExtract("month",false));
		registerCompatible(Func.day, new EmuOracleExtract("day",false));
		registerCompatible(Func.hour, new EmuOracleExtract("hour",true));
		registerCompatible(Func.minute, new EmuOracleExtract("minute",true));
		registerCompatible(Func.second, new EmuOracleExtract("second",true));
		
		registerCompatible(null,EmuOracleToDate.getInstance(),"timestamp");
		registerCompatible(Func.adddate, new EmuOracleDateAdd());
		registerCompatible(Func.subdate, new EmuOracleDateSub());
		registerCompatible(Func.timestampdiff, new EmuOracleTimeStampDiff());
		registerCompatible(Func.timestampadd, new EmuOracleTimestampAdd());
		registerCompatible(Func.cast, new EmuOracleCast());
		
		registerCompatible(Func.datediff, new TemplateFunction("datediff","trunc(%1$s-%2$s)"));
		registerAlias(Func.str, "to_char");
	}
	
	public Object getJavaValue(ColumnType column, Object object) {
		if (object == null)
			return null;

		// boolean类型的解析
		if (column instanceof ColumnType.Boolean) {
			return "1".equals(object.toString());
		}
		// 除此之外的基本类型的值都不需要再单独处理
		if (object instanceof String)
			return object;

		if (object instanceof TIMESTAMP)
			try {
				return ((TIMESTAMP) object).timestampValue();
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
		return object;
	}

	protected String getComment(Varchar column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("varchar2(" + column.length + ")");
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(Double column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("number(" + column.precision + "," + column.scale + ")");
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(column.defaultValue.toString());
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(Int column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("number(" + column.precision + ")");
		if (flag) {
			if (column.defaultValue != null)
				sb.append(" default ").append(column.defaultValue.toString());
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("number(" + column.length + ")");
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	protected String getComment(TimeStamp column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		// 备注：Oracle中date可以表示到秒，timestamp可以表示到秒的小数点后若干位，
		// 此处无必要，但保留Oracle timeStamp的处理以备以后扩展。
		// sb.append("timestamp");
		sb.append("date");
		if (flag) {
			if (column.defaultValue != null) {
				sb.append(" default ").append(toDefaultString(column.defaultValue));
			}
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	public CachedRowSet newCacheRowSetInstance() throws SQLException {
		return new oracle.jdbc.rowset.OracleCachedRowSet();
	}

	public ColumnType getProprtMetaFromDbType(jef.database.meta.Column column) {
		if ("NUMBER".equals(column.getDataType())) {
			if (column.getDecimalDigit() > 0) {// 小数
				return new ColumnType.Double(column.getColumnSize(), column.getDecimalDigit());
			} else {// 整数
				return new ColumnType.Int(column.getColumnSize());
			}
		} else if ("NVARCHAR2".equals(column.getDataType())) {
			if ("GUID".equals(column.getColumnDef())) {
				return new ColumnType.GUID();
			} else {
				return new Varchar(column.getColumnSize());
			}
		} else if ("DATE".equals(column.getDataType())) {
			return new ColumnType.Date();
		} else if (column.getDataType().toUpperCase().startsWith("TIMESTAMP")) { // Oracle
																					// 有
																					// TimeStamp(6)这种类型
			return new ColumnType.TimeStamp();
		} else {
			return super.getProprtMetaFromDbType(column);
		}
	}

	public String getGeneratedFetchFunction() {
		throw new UnsupportedOperationException();
	}

	public String getDriverClass(String url) {
		return "oracle.jdbc.driver.OracleDriver";
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		// jdbc:oracle:thin:@192.168.253.249:1521:ora10
		sb.append(getName()).append(":thin:");
		sb.append("@").append(host).append(":").append(port <= 0 ? 1521 : port);
		sb.append(":").append(pathOrName);

		String url = sb.toString();
		if(ORMConfig.getInstance().isDebugMode()){
			LogUtil.show(url);
		}
		return url;
	}

	public RDBMS getName() {
		return RDBMS.oracle;
	}

	private final static String ORACLE_PAGE1 = "select * from (select tb__.*, rownum rid__ from (";
	private final static String ORACLE_PAGE2 = " ) tb__ where rownum <= %end%) where rid__ >= %start%";

	public String toPageSQL(String sql, IntRange range) {
		sql = ORACLE_PAGE1 + sql;
		String limit = ORACLE_PAGE2.replace("%start%", String.valueOf(range.getLeastValue()));
		limit = limit.replace("%end%", String.valueOf(range.getGreatestValue()));
		sql = sql.concat(limit);
		return sql;
	}

	@Override
	public String getObjectNameIfUppercase(String name) {
		return name==null?null:name.toUpperCase();
	}

	@Override
	public String getColumnNameIncase(String name) {
		return name==null?null:name.toUpperCase();
	}

	@Override
	public Object toBooleanSqlParam(java.lang.Boolean bool) {
		if (bool == null) {
			return "0";
		} else {
			return bool.booleanValue() ? "1" : "0";
		}
	}

	/**
	 * 由于暂不考虑支持Oracle TIMESTAMP到毫秒这个特性，因此在查询时需要对TIMESTAMP进行truncate处理，
	 * 以避免因多了几个毫秒而导致查不到数据的问题。
	 */
	@Override
	public java.sql.Timestamp toTimestampSqlParam(Date timestamp) {
		Calendar gval = Calendar.getInstance();
		gval.setTime(timestamp);
		int mills=gval.get(Calendar.MILLISECOND);
		return new java.sql.Timestamp(timestamp.getTime()-mills);
	}

	/**
	 * 判断SEQUENCE是否存在时，若schema为小写会存在误判情况（将存在判断为不存在）， <br>
	 * 为了避免大小写引起的问题，一律转成大写。
	 */
	@Override
	public String getSchema(String schema) {
		return StringUtils.upperCase(schema);
	}

	@Override
	public int calcSequenceStep(OperateTarget conn, String schema, String seqName, int defaultValue) {
		if (defaultValue > 0)
			return defaultValue;
		if (StringUtils.isBlank(seqName)) {
			LogUtil.warn("Return defaultValue because sequence name is blank on calculating SequenceStep.");
			return defaultValue;
		}
		
		String sql = "select increment_by from all_sequences where sequence_owner like ? and sequence_name = ?";
		
		schema=StringUtils.isBlank(schema) ? "%" : schema.toUpperCase();
		seqName=seqName.toUpperCase();
		try {
			int value=conn.getMetaData().selectBySql(sql, ResultSetTransformer.GET_FIRST_INT, 1, Arrays.asList(schema,seqName));
			LogUtil.info("Tring to access ALL_SEQUENCE to get the increament of sequence [" + seqName+"], the step="+value);
			return value;
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			LogUtil.exception(e);
		}
		return defaultValue;
	}

	@Override
	public String[] getOtherVersionSql() {
//		"select * from nls_database_parameters", 
		return new String[] { "select 'USER_LANGUAGE',userenv('language') from dual" };
	}

	@Override
	public boolean isIOError(SQLException se) {
		int code = se.getErrorCode();
		if (code == 12007 || code == 17410 || code == 17008 || code == 17002 || code == 28 || (code > 1087 && code < 1093)) {
			return true;
		} else if (se.getCause() != null && "NetException".equals(se.getCause().getClass().getSimpleName())) {
			return true;
		} else {
			// 为了跟踪所有Oracle出现网络异常时的错误码，将错误码和异常信息打印出来。
			LogUtil.info("Oracle non-io Err:" + se.getErrorCode() + ":" + se.getMessage());
			return false;
		}
	}

	public Connection createConnection(DataSource ds) throws SQLException {
		// 设置连接属性
		Properties prop = new Properties();
		prop.put("v$session.program", "JefOrm@".concat(ProcessUtil.getHostname()));
		prop.put("v$session.process", String.valueOf(ProcessUtil.getPid()));
		prop.put(oracle.net.ns.SQLnetDef.TCP_CONNTIMEOUT_STR, "4000");
		prop.put(oracle.net.ns.SQLnetDef.TCP_READTIMEOUT_STR, "30000");
		if (ds instanceof SimpleDataSource) {
			return ((SimpleDataSource) ds).getConnectionFromDriver(prop);
		} else {
			return ds.getConnection();
		}
	}

	@Override
	public void processConnectProperties(DataSourceInfo dsw) {
		dsw.putProperty("v$session.program", "JefOrm@".concat(ProcessUtil.getHostname()));
		dsw.putProperty("v$session.process", String.valueOf(ProcessUtil.getPid()));
		dsw.putProperty(oracle.net.ns.SQLnetDef.TCP_CONNTIMEOUT_STR, "4000");
		dsw.putProperty(oracle.net.ns.SQLnetDef.TCP_READTIMEOUT_STR, "30000");
	}

	static final char[] CHAR_OF_END = new char[] { ':' };
	static final char[] CHAR_OF_END2 = new char[] { ':', ' ' };

	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consume("jdbc:", ' ');
		reader.consume("oracle:", ' ');
		if (reader.matchNext("oci:", ' ') > -1) {// 按OCI处理
			reader.consume("oci:@", ' ');
			reader.omitChars('/');
			String token = reader.readToken(CHAR_OF_END2);
			if (reader.eof()) {
				connectInfo.setDbname(token);
				connectInfo.setHost(token);
			} else {
				String port = reader.readToken(CHAR_OF_END);
				if (port.length() > 0) {
					token = token + ":" + port;
				}
				connectInfo.setHost(token);
				String dbname = reader.readToken(CHAR_OF_END2);
				connectInfo.setDbname(dbname);
			}
		} else {// 按thin处理
			reader.consume("thin:@", ' ');// RAC连接串
			if (reader.matchNextIgnoreCase("(DESCRIPTION") > -1) {//
				reader.omitAfterKeyIgnoreCase("(HOST=", ' ');
				String host = reader.readToken('"', ')');
				reader.omitAfterKeyIgnoreCase("(SERVICE_NAME=", ' ');
				String dbName = reader.readToken('"', ')');
				connectInfo.setHost(host.trim());
				connectInfo.setDbname(dbName.trim());
			} else {// 通常串
				reader.omitChars('/');
				String host = reader.readToken(CHAR_OF_END);
				String port = reader.readToken(CHAR_OF_END);
				String dbname = reader.readToken(CHAR_OF_END2);
				if (port.length() > 0)
					host = host + ":" + port;
				connectInfo.setHost(host);
				connectInfo.setDbname(dbname);
			}
		}
	}

	@Override
	public void processIntervalExpression(BinaryExpression parent, Interval interval) {
		String unit=interval.getUnit().toLowerCase();
		interval.toMySqlMode();
		Expression value=interval.getValue();
		if("day".equals(unit)){
			replace(parent,interval,value);
		}else if("hour".equals(unit)){
			value=new Division(value,new LongValue(24));
			value=new Parenthesis(value);
			replace(parent,interval,value);
		}else if("minute".equals(unit)){
			value=new Division(value,new LongValue(1440));
			value=new Parenthesis(value);
			replace(parent,interval,value);
		}else if("second".equals(unit)){
			value=new Division(value,new LongValue(86400));
			value=new Parenthesis(value);
			replace(parent,interval,value);
		}else if("month".equals(value)){
			if(parent.getLeftExpression()==interval){
				parent.swap();//交换到右边
			}
			Function func=new Function("add_months",parent.getLeftExpression(),parent.getRightExpression());
			parent.rewrite=func;
		}else if("year".equals(value)){
			if(parent.getLeftExpression()==interval){
				parent.swap();//交换到右边
			}
			Expression right=new Multiplication(parent.getRightExpression(),new LongValue(12));
			Function func=new Function("add_months",parent.getLeftExpression(),right);
			parent.rewrite=func;
		}else{
			throw new UnsupportedOperationException("The Oracle Dialect can't handle datetime unit "+unit+" for now.");
		}
	}

	private void replace(BinaryExpression parent, Interval interval, Expression value) {
		if(parent.getLeftExpression()==interval){
			parent.setLeftExpression(value);
		}else{
			parent.setRightExpression(value);
		}
	}
	@Override
	public String getSqlDateExpression(Date value) {
		return "to_date(" + QUOT + DateUtils.formatDate(value) + QUOT + ",'YYYY-MM-DD')";
	}

	@Override
	public String getSqlTimeExpression(Date value) {
		return "to_date(" + QUOT + DateFormats.TIME_ONLY.get().format(value) + QUOT + ",'HH24:MI:SS')";
	}

	@Override
	public String getSqlTimestampExpression(Date value) {
		return "to_date(" + QUOT + DateUtils.formatDateTime(value) + QUOT + ",'YYYY-MM-dd HH24:MI:SS')";
	}

	public void addKeyword(String... keys) {
		for(String s:keys){
			this.keywords.add(s.toUpperCase());
		}
	}
}
