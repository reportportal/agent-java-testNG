package com.epam.reportportal.testng.step;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import rp.com.google.common.base.Function;
import rp.com.google.common.collect.Queues;
import rp.com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.UUID;

import static com.sun.org.apache.xml.internal.utils.LocaleUtility.EMPTY_STRING;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporter {

	private static StepReporter instance;

	private final ThreadLocal<Launch> launch;

	private final ThreadLocal<Deque<Maybe<String>>> parents;

	private StepReporter() {
		launch = new InheritableThreadLocal<Launch>();
		parents = new InheritableThreadLocal<Deque<Maybe<String>>>() {
			@Override
			protected Deque<Maybe<String>> initialValue() {
				return Queues.newArrayDeque();
			}
		};
	}

	public static StepReporter getInstance() {
		if (instance == null) {
			synchronized (StepReporter.class) {
				if (instance == null) {
					instance = new StepReporter();
				}
			}
		}
		return instance;
	}

	public void setLaunch(Launch launch) {
		this.launch.set(launch);
	}

	public void addParent(Maybe<String> parent) {
		this.parents.get().push(parent);
	}

	public Maybe<String> removeParent() {
		return this.parents.get().poll();
	}

	public void sendStep(String name) {
		Maybe<String> stepId = startStepRequest(name);
		finishStepRequest(stepId, "PASSED");
	}

	public void sendStep(String status, String name) {
		Maybe<String> stepId = startStepRequest(name);
		finishStepRequest(stepId, status);
	}

	public void sendStep(String status, String name, final Throwable throwable) {
		Maybe<String> stepId = startStepRequest(name);

		ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				return buildSaveLogRequest(itemId, "ERROR", throwable);
			}
		});

		finishStepRequest(stepId, status);
	}

	public void sendStep(String name, final File file) {
		Maybe<String> stepId = startStepRequest(name);
		ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				return buildSaveLogRequest(itemId, EMPTY_STRING, "INFO", file);
			}
		});
		finishStepRequest(stepId, "PASSED");
	}

	public void sendStep(String status, String name, final File file) {
		Maybe<String> stepId = startStepRequest(name);
		ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				return buildSaveLogRequest(itemId, EMPTY_STRING, "INFO", file);
			}
		});
		finishStepRequest(stepId, status);
	}

	public void sendStep(String status, String name, final File file, final Throwable throwable) {
		Maybe<String> stepId = startStepRequest(name);

		ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				return buildSaveLogRequest(itemId, "ERROR", file, throwable);
			}
		});

		finishStepRequest(stepId, status);
	}

	private Maybe<String> startStepRequest(String name) {
		StartTestItemRQ startTestItemRQ = buildStartStepRequest(name);
		return launch.get().startTestItem(parents.get().peek(), startTestItemRQ);
	}

	private void finishStepRequest(Maybe<String> stepId, String status) {
		FinishTestItemRQ finishTestItemRQ = buildFinishTestItemRequest(status, Calendar.getInstance().getTime());
		launch.get().finishTestItem(stepId, finishTestItemRQ);
	}

	private StartTestItemRQ buildStartStepRequest(String name) {
		StartTestItemRQ startTestItemRQ = new StartTestItemRQ();
		startTestItemRQ.setName(name);
		startTestItemRQ.setType("STEP");
		startTestItemRQ.setHasStats(false);
		startTestItemRQ.setStartTime(Calendar.getInstance().getTime());
		return startTestItemRQ;
	}

	private FinishTestItemRQ buildFinishTestItemRequest(String status, Date endTime) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status);
		finishTestItemRQ.setEndTime(endTime);
		return finishTestItemRQ;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String message, String level) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setTestItemId(itemId);
		rq.setMessage(message);
		rq.setLevel(level);
		rq.setLogTime(Calendar.getInstance().getTime());
		return rq;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String message, String level, File file) {
		SaveLogRQ logRQ = buildSaveLogRequest(itemId, message, level);
		if (file.exists()) {
			try {
				logRQ.setFile(createFileModel(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return logRQ;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String level, Throwable throwable) {
		String message = throwable != null ? getStackTraceAsString(throwable) : "Test has failed without exception";
		return buildSaveLogRequest(itemId, message, level);
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String level, File file, Throwable throwable) {
		String message = throwable != null ? getStackTraceAsString(throwable) : "Test has failed without exception";
		SaveLogRQ rq = buildSaveLogRequest(itemId, message, level);

		if (file.exists()) {
			try {
				rq.setFile(createFileModel(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return rq;
	}

	private SaveLogRQ.File createFileModel(File file) throws IOException {
		TypeAwareByteSource data = new TypeAwareByteSource(Files.asByteSource(file), MimeTypeDetector.detect(file));
		SaveLogRQ.File fileModel = new SaveLogRQ.File();
		fileModel.setContent(data.read());
		fileModel.setContentType(data.getMediaType());
		fileModel.setName(UUID.randomUUID().toString());
		return fileModel;
	}
}
