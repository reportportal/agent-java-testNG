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

import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporter {

	private static StepReporter instance;

	private Launch launch;

	private final ThreadLocal<Maybe<String>> parent;

	private final ThreadLocal<Deque<StepEntry>> steps;

	private final ThreadLocal<Set<Maybe<String>>> parentFailures;

	private StepReporter() {
		parent = new InheritableThreadLocal<Maybe<String>>() {
			@Override
			protected Maybe<String> initialValue() {
				return Maybe.empty();
			}
		};
		steps = new InheritableThreadLocal<Deque<StepEntry>>() {

			@Override
			protected Deque<StepEntry> initialValue() {
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

	private static class StepEntry {
		private final Maybe<String> itemId;
		private final FinishTestItemRQ finishTestItemRQ;

		private StepEntry(Maybe<String> itemId, FinishTestItemRQ finishTestItemRQ) {
			this.itemId = itemId;
			this.finishTestItemRQ = finishTestItemRQ;
		}

		public Maybe<String> getItemId() {
			return itemId;
		}

		public FinishTestItemRQ getFinishTestItemRQ() {
			return finishTestItemRQ;
		}
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
		this.launch = launch;
	}

	public void setParent(Maybe<String> parent) {
		if (parent != null) {
			this.parent.set(parent);
		}
	}

	public Maybe<String> removeParent() {
		Maybe<String> parent = this.parent.get();
		this.parent.set(Maybe.empty());
		return parent;
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

	public void finishPreviousStep() {
		ofNullable(steps.get().poll()).ifPresent(stepEntry -> launch.finishTestItem(stepEntry.getItemId(),
				stepEntry.getFinishTestItemRQ()
		));
	}

	private Maybe<String> startStepRequest(String name) {
		finishPreviousStep();
		StartTestItemRQ startTestItemRQ = buildStartStepRequest(name);
		return launch.startTestItem(parent.get(), startTestItemRQ);
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
		steps.get().add(new StepEntry(stepId, finishTestItemRQ));
		if ("FAILED".equalsIgnoreCase(status)) {
			parentFailures.get().add(parent.get());
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
		rq.setItemUuid(itemId);
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
