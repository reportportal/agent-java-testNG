package com.epam.reportportal.testng.util;

import com.beust.jcommander.internal.Lists;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rp.com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static com.epam.reportportal.testng.TestNGService.REPORT_PORTAL;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeReporter {

	private ItemTreeReporter() {
		//static only
	}

	public static boolean sendLog(final String level, final String message, final Date logTime, TestItemTree.TestItemLeaf testItemLeaf) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (itemId != null) {
			sendLogRequest(itemId, level, message, logTime).subscribe();
			return true;
		} else {
			return false;
		}
	}

	public static boolean sendLog(final String level, final String message, final Date logTime, TestItemTree.TestItemLeaf testItemLeaf,
			Consumer<EntryCreatedAsyncRS> batchSaveOperatingRSConsumer) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (itemId != null) {
			sendLogRequest(itemId, level, message, logTime).subscribe(batchSaveOperatingRSConsumer);
			return true;
		} else {
			return false;
		}
	}

	public static boolean sendLog(final String level, final String message, final Date logTime, final File file,
			TestItemTree.TestItemLeaf testItemLeaf) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (itemId != null) {
			sendLogMultiPartRequest(itemId, level, message, logTime, file).subscribe();
			return true;
		} else {
			return false;
		}
	}

	public static boolean sendLog(final String level, final String message, final Date logTime, final File file,
			TestItemTree.TestItemLeaf testItemLeaf, Consumer<BatchSaveOperatingRS> batchSaveOperatingRSConsumer) {
		Maybe<String> itemId = testItemLeaf.getItemId();
		if (itemId != null) {
			sendLogMultiPartRequest(itemId, level, message, logTime, file).subscribe(batchSaveOperatingRSConsumer);
			return true;
		} else {
			return false;
		}
	}

	private static Maybe<EntryCreatedAsyncRS> sendLogRequest(Maybe<String> itemId, final String level, final String message,
			final Date logTime) {
		return itemId.flatMap(new Function<String, MaybeSource<EntryCreatedAsyncRS>>() {
			@Override
			public MaybeSource<EntryCreatedAsyncRS> apply(String itemId) {
				SaveLogRQ saveLogRequest = createSaveLogRequest(itemId, level, message, logTime);
				return REPORT_PORTAL.getClient().log(saveLogRequest);
			}
		}).observeOn(Schedulers.computation());
	}

	private static Maybe<BatchSaveOperatingRS> sendLogMultiPartRequest(Maybe<String> itemId, final String level, final String message,
			final Date logTime, final File file) {
		return itemId.flatMap(new Function<String, MaybeSource<BatchSaveOperatingRS>>() {
			@Override
			public MaybeSource<BatchSaveOperatingRS> apply(String itemId) throws Exception {
				SaveLogRQ saveLogRequest = createSaveLogRequest(itemId, level, message, logTime);
				saveLogRequest.setFile(createFileModel(file));
				MultiPartRequest multiPartRequest = HttpRequestUtils.buildLogMultiPartRequest(Lists.newArrayList(saveLogRequest));
				return REPORT_PORTAL.getClient().log(multiPartRequest);
			}
		}).observeOn(Schedulers.computation());
	}

	private static SaveLogRQ createSaveLogRequest(String itemId, String level, String message, Date logTime) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		saveLogRQ.setItemUuid(itemId);
		saveLogRQ.setLevel(level);
		saveLogRQ.setLogTime(logTime);
		saveLogRQ.setMessage(message);
		return saveLogRQ;
	}

	private static SaveLogRQ.File createFileModel(File file) throws IOException {
		TypeAwareByteSource data = new TypeAwareByteSource(Files.asByteSource(file), MimeTypeDetector.detect(file));
		SaveLogRQ.File fileModel = new SaveLogRQ.File();
		fileModel.setContent(data.read());
		fileModel.setContentType(data.getMediaType());
		fileModel.setName(file.getName());
		return fileModel;
	}
}
