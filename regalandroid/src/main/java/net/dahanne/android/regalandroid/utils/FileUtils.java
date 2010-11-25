/**
 *  ReGalAndroid, a gallery client for Android, supporting G2, G3, etc...
 *  URLs: https://github.com/anthonydahanne/ReGalAndroid , http://blog.dahanne.net
 *  Copyright (c) 2010 Anthony Dahanne
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.dahanne.android.regalandroid.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.dahanne.android.regalandroid.R;
import net.dahanne.android.regalandroid.activity.Settings;
import net.dahanne.android.regalandroid.remote.RemoteGalleryConnectionFactory;
import net.dahanne.gallery.commons.model.Picture;
import net.dahanne.gallery.commons.remote.GalleryConnectionException;
import net.dahanne.gallery.commons.remote.RemoteGallery;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Environment;

public class FileUtils {

	private static final String NO_CACHE_PATH = "/.nomedia";
	private static FileUtils fileUtils = new FileUtils();
	private final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	public static FileUtils getInstance() {
		return fileUtils;
	}

	private FileUtils() {

	}

	private final RemoteGallery remoteGallery = RemoteGalleryConnectionFactory
			.getInstance();

	/**
	 * download the requested file from the gallery, and save it to cache
	 * 
	 * @param context
	 * @param fileName
	 * @param extension
	 * @param imageUrl
	 * @param isTemporary
	 * @return
	 * @throws GalleryConnectionException
	 * @throws FileHandlingException
	 */
	public File getFileFromGallery(Context context, String fileName,
			String extension, String imageUrl, boolean isTemporary,
			int albumName) throws GalleryConnectionException,
			FileHandlingException {
		logger.debug(
				"gettingFileFromGallery, fileName : {} -- extension : {} -- imageUrl : {} -- isTemporary : {} -- albumName : {}",
				new Object[] { fileName, extension, imageUrl, isTemporary,
						albumName });
		File imageFileOnExternalDirectory = null;
		try {
			InputStream inputStreamFromUrl = null;
			String storageState = Environment.getExternalStorageState();
			if (storageState.contains("mounted")) {
				logger.debug("storage is mounted");
				File savePath = new File(
						Settings.getG2AndroidCachePath(context) + "/"
								+ albumName);
				// if the cache has never been used before
				if (!savePath.exists()) {
					// we make sure g2android path exists (/g2android)
					File g2AndroidDirectory = new File(
							Settings.getG2AndroidPath(context));
					g2AndroidDirectory.mkdir();
					// we then create g2android cache path (tmp)
					File g2AndroidCacheDirectory = new File(
							Settings.getG2AndroidCachePath(context));
					g2AndroidCacheDirectory.mkdir();
					// and also that the specific album folder exists, bug #65
					File albumCacheDirectory = new File(
							Settings.getG2AndroidCachePath(context) + "/"
									+ albumName);
					albumCacheDirectory.mkdir();

					// issue #30 : insert the .nomedia file so that the dir
					// won't be parsed by other photo apps
					File noMediaFile = new File(
							Settings.getG2AndroidCachePath(context) + "/"
									+ albumName + NO_CACHE_PATH);
					if (!noMediaFile.createNewFile()) {
						throw new FileHandlingException(
								context.getString(R.string.external_storage_problem));
					}
				}
				// if the file downloaded is not a cache file, but a file to
				// keep
				if (!isTemporary) {
					savePath = new File(Settings.getG2AndroidPath(context));
					// if there is no file extension, we add the one that
					// corresponds to the picture (if we have it)
					if (fileName.lastIndexOf(".") == -1
							&& !StringUtils.isEmpty(extension)) {
						fileName = fileName + "." + extension;
					}
				}
				logger.debug("savePath is : {}", savePath);
				imageFileOnExternalDirectory = new File(savePath, fileName);
				inputStreamFromUrl = remoteGallery
						.getInputStreamFromUrl(imageUrl);
			} else {
				throw new FileHandlingException(
						context.getString(R.string.external_storage_problem));
			}
			FileOutputStream fos;
			fos = new FileOutputStream(imageFileOnExternalDirectory);
			byte[] buf = new byte[1024];
			int len;
			while ((len = inputStreamFromUrl.read(buf)) > 0) {
				fos.write(buf, 0, len);
			}
			fos.close();
			inputStreamFromUrl.close();

		} catch (FileNotFoundException e) {
			throw new FileHandlingException(e.getMessage());
		} catch (IOException e) {
			throw new FileHandlingException(e.getMessage());
		}
		return imageFileOnExternalDirectory;
	}

	public void clearCache(Context context) {
		logger.debug("clearingCache");
		File tempG2AndroidPath = new File(
				Settings.getG2AndroidCachePath(context));
		if (tempG2AndroidPath.exists()) {
			for (File file : tempG2AndroidPath.listFiles()) {
				file.delete();
			}
			tempG2AndroidPath.delete();
		}

	}

	/**
	 * 
	 * issue #23 : when there is no resized picture, we fetch the original
	 * picture
	 * 
	 * @param picture
	 * @return
	 */
	public String chooseBetweenResizedAndOriginalUrl(Picture picture) {
		logger.debug("picture : {}", picture);
		String resizedName = picture.getResizedUrl();
		if (resizedName == null) {
			resizedName = picture.getFileUrl();
		}
		return resizedName;
	}

}
