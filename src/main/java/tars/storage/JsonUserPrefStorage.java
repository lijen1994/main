package tars.storage;

import tars.commons.core.LogsCenter;
import tars.commons.exceptions.DataConversionException;
import tars.commons.util.FileUtil;
import tars.commons.util.StringUtil;
import tars.model.UserPrefs;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * A class to access UserPrefs stored in the hard disk as a json file
 */
public class JsonUserPrefStorage implements UserPrefsStorage {

    private static final Logger logger =
            LogsCenter.getLogger(JsonUserPrefStorage.class);

    private static String LOG_MESSAGE_PREF_FILE_NOT_FOUND =
            "Prefs file %s not found";
    private static String LOG_MESSAGE_PREF_FILE_READING_ERROR =
            "Error reading from prefs file %1$s: %2$s";
    private String filePath;

    public JsonUserPrefStorage(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Optional<UserPrefs> readUserPrefs()
            throws DataConversionException, IOException {
        return readUserPrefs(filePath);
    }

    @Override
    public void saveUserPrefs(UserPrefs userPrefs) throws IOException {
        saveUserPrefs(userPrefs, filePath);
    }

    /**
     * Similar to {@link #readUserPrefs()}
     * 
     * @param prefsFilePath location of the data. Cannot be null.
     * @throws DataConversionException if the file format is not as expected.
     */
    public Optional<UserPrefs> readUserPrefs(String prefsFilePath)
            throws DataConversionException {
        assert prefsFilePath != null;

        File prefsFile = new File(prefsFilePath);

        if (!prefsFile.exists()) {
            logger.info(
                    String.format(LOG_MESSAGE_PREF_FILE_NOT_FOUND, prefsFile));
            return Optional.empty();
        }

        UserPrefs prefs;

        try {
            prefs = FileUtil.deserializeObjectFromJsonFile(prefsFile,
                    UserPrefs.class);
        } catch (IOException e) {
            logger.warning(String.format(LOG_MESSAGE_PREF_FILE_READING_ERROR,
                    prefsFile, e));
            throw new DataConversionException(e);
        }

        return Optional.of(prefs);
    }

    /**
     * Similar to {@link #saveUserPrefs(UserPrefs)}
     * 
     * @param prefsFilePath location of the data. Cannot be null.
     */
    public void saveUserPrefs(UserPrefs userPrefs, String prefsFilePath)
            throws IOException {
        assert userPrefs != null;
        assert prefsFilePath != null;

        FileUtil.serializeObjectToJsonFile(new File(prefsFilePath), userPrefs);
    }
}
