/**
 * 
 */
package com.kyj.stockdaily;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

/**
 * @author KYJ
 *
 */
public class StockDaily {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new StockDaily().action();
	}

	private void action() throws Exception {
		System.out.println("application start");
		File file = new File("daily.sql");
		if (!file.exists()) {
			throw new FileNotFoundException("daily.sql does not exists.");
		}
		String startDate = getStartDate();
		String endDate = getEndDate();
		
		
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File("sys.properties")));
		
		
		String dbUrl = properties.getProperty("db.url");
		String dbUserName = properties.getProperty("db.username");
		String dbUserPwd = properties.getProperty("db.userpwd");
		String outFileName = properties.getProperty("out.file.name", "out.csv");
		String sql = Files.readString(file.toPath());
		StringBuilder sb = new StringBuilder();
		try (Connection con = DriverManager.getConnection(dbUrl, dbUserName, dbUserPwd)) {
			PreparedStatement statement = con.prepareStatement(sql);
			statement.setString(1, startDate);
			statement.setString(2, endDate);
			statement.setString(3, startDate);
			statement.setString(4, endDate);
			try (ResultSet rs = statement.executeQuery()) {
				int columnCount = rs.getMetaData().getColumnCount();

				
				for (int i = 1; i <= columnCount; i++) {
					sb.append(rs.getMetaData().getColumnName(i)).append("\t");
				}
				sb.append(System.lineSeparator());
				while (rs.next()) {
					for (int i = 1; i <= columnCount; i++) {
						sb.append("\"").append(rs.getString(i)).append("\"").append("\t");
					}
					sb.append(System.lineSeparator());
				}
			}
		}
		File out = new File(outFileName);
		Files.deleteIfExists(out.toPath());
		String string = sb.toString();
		System.out.println(string);
		Files.write(out.toPath(), string.getBytes("utf-8"), 
				StandardOpenOption.CREATE_NEW, 
				StandardOpenOption.WRITE);
		System.out.println("application end");
	}

	/**
	 * @return
	 */
	private String getEndDate() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		return simpleDateFormat.format(new Date());
	}

	private String getStartDate() {
		Calendar instance = Calendar.getInstance();
		instance.add(Calendar.DAY_OF_MONTH, -7);
		Date time = instance.getTime();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return simpleDateFormat.format(time);
	}
}
