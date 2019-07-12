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
import rp.com.google.common.collect.Sets;
import rp.com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporter {

	private static StepReporter instance;

	private final ThreadLocal<Launch> launch;

	private final ThreadLocal<Deque<Maybe<String>>> parents;

	private final ThreadLocal<Set<Maybe<String>>> parentFailures;

	private StepReporter() {
		launch = new InheritableThreadLocal<Launch>();
		parents = new InheritableThreadLocal<Deque<Maybe<String>>>() {
			@Override
			protected Deque<Maybe<String>> initialValue() {
				return Queues.newArrayDeque();
			}
		};
		parentFailures = new ThreadLocal<Set<Maybe<String>>>() {
			@Override
			protected Set<Maybe<String>> initialValue() {
				return Sets.newHashSet();
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

	public boolean isParentFailed(Maybe<String> parentId) {
		if (parentFailures.get().contains(parentId)) {
			parentFailures.get().remove(parentId);
			return true;
		}
		return false;
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

	public void sendStep(String name, final File... files) {
		Maybe<String> stepId = startStepRequest(name);
		for (final File file : files) {
			ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
				@Override
				public SaveLogRQ apply(String itemId) {
					return buildSaveLogRequest(itemId, file.getName(), "INFO", file);
				}
			});
		}
		finishStepRequest(stepId, "PASSED");
	}

	public void sendStep(String status, String name, final File... files) {
		Maybe<String> stepId = startStepRequest(name);
		for (final File file : files) {
			ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
				@Override
				public SaveLogRQ apply(String itemId) {
					return buildSaveLogRequest(itemId, file.getName(), "INFO", file);
				}
			});
		}
		finishStepRequest(stepId, status);
	}

	public void sendStep(String status, String name, final Throwable throwable, final File... files) {
		Maybe<String> stepId = startStepRequest(name);
		for (final File file : files) {
			ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
				@Override
				public SaveLogRQ apply(String itemId) {
					return buildSaveLogRequest(itemId, "ERROR", throwable, file);
				}
			});
		}
		finishStepRequest(stepId, status);
	}

	private Maybe<String> startStepRequest(String name) {
		StartTestItemRQ startTestItemRQ = buildStartStepRequest(name);
		return launch.get().startTestItem(parents.get().peek(), startTestItemRQ);
	}

	private StartTestItemRQ buildStartStepRequest(String name) {
		StartTestItemRQ startTestItemRQ = new StartTestItemRQ();
		startTestItemRQ.setName(name);
		startTestItemRQ.setType("STEP");
		startTestItemRQ.setHasStats(false);
		startTestItemRQ.setStartTime(Calendar.getInstance().getTime());
		return startTestItemRQ;
	}

	private void finishStepRequest(Maybe<String> stepId, String status) {
		FinishTestItemRQ finishTestItemRQ = buildFinishTestItemRequest(status, Calendar.getInstance().getTime());
		launch.get().finishTestItem(stepId, finishTestItemRQ);
		if ("FAILED".equalsIgnoreCase(status)) {
			Maybe<String> parentId = parents.get().peek();
			parentFailures.get().add(parentId);
		}
	}

	private FinishTestItemRQ buildFinishTestItemRequest(String status, Date endTime) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status);
		finishTestItemRQ.setEndTime(endTime);
		return finishTestItemRQ;
	}

	private SaveLogRQ buildSaveLogRequest(String itemId, String message, String level) {
		SaveLogRQ rq = new SaveLogRQ();
		rq.setItemId(itemId);
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

	private SaveLogRQ buildSaveLogRequest(String itemId, String level, Throwable throwable, File file) {
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
