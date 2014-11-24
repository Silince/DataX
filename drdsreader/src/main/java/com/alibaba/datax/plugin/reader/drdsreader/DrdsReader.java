package com.alibaba.datax.plugin.reader.drdsreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.rdbms.reader.CommonRdbmsReader;
import com.alibaba.datax.plugin.rdbms.reader.Constant;
import com.alibaba.datax.plugin.rdbms.reader.Key;
import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.datax.plugin.rdbms.util.TableExpandUtil;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrdsReader extends Reader {

	private static final DataBaseType DATABASE_TYPE = DataBaseType.MySql;
	private static final Logger LOG = LoggerFactory.getLogger(DrdsReader.class);

	public static class Master extends Reader.Master {

		private Configuration originalConfig = null;
		private CommonRdbmsReader.Master commonRdbmsReaderMaster;

		@Override
		public void init() {
			this.originalConfig = super.getPluginJobConf();
			int fetchSize = this.originalConfig.getInt(Constant.FETCH_SIZE,
					Integer.MIN_VALUE);
			this.originalConfig.set(Constant.FETCH_SIZE, fetchSize);
			this.validateConfiguration();

			this.commonRdbmsReaderMaster = new CommonRdbmsReader.Master(
					DATABASE_TYPE);
			this.commonRdbmsReaderMaster.init(this.originalConfig);
		}

		@Override
		public List<Configuration> split(int adviceNumber) {
			return DrdsReaderSplitUtil.doSplit(this.originalConfig,
					adviceNumber);
		}

		@Override
		public void post() {
			this.commonRdbmsReaderMaster.post(this.originalConfig);
		}

		@Override
		public void destroy() {
			this.commonRdbmsReaderMaster.destroy(this.originalConfig);
		}

		private void validateConfiguration() {
			// do not splitPk
			String splitPk = originalConfig.getString(Key.SPLIT_PK, null);
			if (null != splitPk) {
				LOG.warn("由于您读取数据库是drds, 所以您不需要配置 splitPk. 如果您不想看到这条提醒，请移除您源头表中配置的 splitPk.");
				this.originalConfig.remove(Key.SPLIT_PK);
			}

			List<Object> conns = this.originalConfig.getList(
					Constant.CONN_MARK, Object.class);
			if (null == conns || conns.size() != 1) {
				throw DataXException.asDataXException(
						DBUtilErrorCode.REQUIRED_VALUE,
						"您未配置读取数据库jdbcUrl的信息. 正确的配置方式是给 jdbcUrl 配置上您需要读取的连接.");
			}
			Configuration connConf = Configuration
					.from(conns.get(0).toString());
			connConf.getNecessaryValue(Key.JDBC_URL,
					DBUtilErrorCode.REQUIRED_VALUE);

			// only one jdbcUrl
			List<String> jdbcUrls = connConf
					.getList(Key.JDBC_URL, String.class);
			if (null == jdbcUrls || jdbcUrls.size() != 1) {
				throw DataXException.asDataXException(
						DBUtilErrorCode.ILLEGAL_VALUE,
						"您配置读取数据库jdbcUrl的数量不正确. 正确的配置方式是配置且只配置 1 个目的 jdbcUrl.");
			}
			// if have table,only one
			List<String> tables = connConf.getList(Key.TABLE, String.class);
			if (null != tables && tables.size() != 1) {
				throw DataXException
						.asDataXException(DBUtilErrorCode.ILLEGAL_VALUE,
								"由于您读取数据库是drds,配置读取源表数目错误. 正确的配置方式是配置且只配置 1 个目的 table.");

			}
			if (null != tables && tables.size() == 1) {
				List<String> expandedTables = TableExpandUtil.expandTableConf(
						DATABASE_TYPE, tables);
				if (null == expandedTables || expandedTables.size() != 1) {
					throw DataXException
							.asDataXException(DBUtilErrorCode.ILLEGAL_VALUE,
									"由于您读取数据库是drds,配置读取源表数目错误. 正确的配置方式是配置且只配置 1 个目的 table.");
				}
			}

			// if have querySql,only one
			List<String> querySqls = connConf.getList(Key.QUERY_SQL,
					String.class);
			if (null != querySqls && querySqls.size() != 1) {
				throw DataXException
						.asDataXException(DBUtilErrorCode.ILLEGAL_VALUE,
								"由于您读取数据库是drds,配置读取querySql数目错误. 正确的配置方式是配置且只配置 1 个目的 querySql.");
			}

			// warn:other checking about table,querySql in common
		}
	}

	public static class Slave extends Reader.Slave {

		private Configuration readerSliceConfig;
		private CommonRdbmsReader.Slave commonRdbmsReaderSlave;

		@Override
		public void init() {
			this.readerSliceConfig = super.getPluginJobConf();
			this.commonRdbmsReaderSlave = new CommonRdbmsReader.Slave(
					DATABASE_TYPE);
			this.commonRdbmsReaderSlave.init(this.readerSliceConfig);

		}

		@Override
		public void startRead(RecordSender recordSender) {
			int fetchSize = this.readerSliceConfig.getInt(Constant.FETCH_SIZE);

			this.commonRdbmsReaderSlave.startRead(this.readerSliceConfig,
					recordSender, super.getSlavePluginCollector(), fetchSize);
		}

		@Override
		public void post() {
			this.commonRdbmsReaderSlave.post(this.readerSliceConfig);
		}

		@Override
		public void destroy() {
			this.commonRdbmsReaderSlave.destroy(this.readerSliceConfig);
		}

	}

}
