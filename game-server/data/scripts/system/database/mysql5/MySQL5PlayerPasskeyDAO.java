package mysql5;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL5DAOUtils;
import com.aionemu.gameserver.dao.PlayerPasskeyDAO;

/**
 * @author cura
 */
public class MySQL5PlayerPasskeyDAO extends PlayerPasskeyDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL5PlayerPasskeyDAO.class);
	public static final String INSERT_QUERY = "INSERT INTO `player_passkey` (`account_id`, `passkey`) VALUES (?,?)";
	public static final String UPDATE_QUERY = "UPDATE `player_passkey` SET `passkey`=? WHERE `account_id`=? AND `passkey`=?";
	public static final String UPDATE_FORCE_QUERY = "UPDATE `player_passkey` SET `passkey`=? WHERE `account_id`=?";
	public static final String CHECK_QUERY = "SELECT COUNT(*) cnt FROM `player_passkey` WHERE `account_id`=? AND `passkey`=?";
	public static final String EXIST_CHECK_QUERY = "SELECT COUNT(*) cnt FROM `player_passkey` WHERE `account_id`=?";

	@Override
	public void insertPlayerPasskey(int accountId, String passkey) {
		try {
			try (Connection con = DatabaseFactory.getConnection(); PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
				stmt.setInt(1, accountId);
				stmt.setString(2, passkey);
				stmt.execute();
			}
		}
		catch (SQLException e) {
			log.error("Error saving PlayerPasskey. accountId: " + accountId, e);
		}
	}

	@Override
	public boolean updatePlayerPasskey(int accountId, String oldPasskey, String newPasskey) {
		boolean result = false;
		try {
			try (Connection con = DatabaseFactory.getConnection(); PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
				stmt.setString(1, newPasskey);
				stmt.setInt(2, accountId);
				stmt.setString(3, oldPasskey);
				if (stmt.executeUpdate() > 0)
					result = true;
			}
		}
		catch (SQLException e) {
			log.error("Error updating PlayerPasskey. accountId: " + accountId, e);
		}
		return result;
	}

	@Override
	public boolean updateForcePlayerPasskey(int accountId, String newPasskey) {
		boolean result = false;
		try {
			try (Connection con = DatabaseFactory.getConnection(); PreparedStatement stmt = con.prepareStatement(UPDATE_FORCE_QUERY)) {
				stmt.setString(1, newPasskey);
				stmt.setInt(2, accountId);
				if (stmt.executeUpdate() > 0)
					result = true;
			}
		}
		catch (SQLException e) {
			log.error("Error updaing PlayerPasskey. accountId: " + accountId, e);
		}
		return result;
	}

	@Override
	public boolean checkPlayerPasskey(int accountId, String passkey) {
		boolean passkeyChecked = false;
		try {
			try (Connection con = DatabaseFactory.getConnection(); PreparedStatement stmt = con.prepareStatement(CHECK_QUERY)) {
				stmt.setInt(1, accountId);
				stmt.setString(2, passkey);
				try (ResultSet rset = stmt.executeQuery()) {
					if (rset.next()) {
						if (rset.getInt("cnt") == 1)
							passkeyChecked = true;
					}
				}
			}
		}
		catch (SQLException e) {
			log.error("Error loading PlayerPasskey. accountId: " + accountId, e);
			return false;
		}
		return passkeyChecked;
	}

	@Override
	public boolean existCheckPlayerPasskey(int accountId) {
		boolean existPasskeyChecked = false;
		try {
			try (Connection con = DatabaseFactory.getConnection(); PreparedStatement stmt = con.prepareStatement(EXIST_CHECK_QUERY)) {
				stmt.setInt(1, accountId);
				try (ResultSet rset = stmt.executeQuery()) {
					if (rset.next()) {
						if (rset.getInt("cnt") == 1)
							existPasskeyChecked = true;
					}
				}
			}
		}
		catch (SQLException e) {
			log.error("Error loading PlayerPasskey. accountId: " + accountId, e);
			return false;
		}
		return existPasskeyChecked;
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL5DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
