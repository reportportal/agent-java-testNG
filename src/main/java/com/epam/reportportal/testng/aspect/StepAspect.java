package com.epam.reportportal.testng.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.testng.ITestNGService;
import com.epam.reportportal.testng.ReportPortalTestNGListener;
import com.epam.reportportal.utils.StepTemplateUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import io.reactivex.annotations.Nullable;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import rp.com.google.common.base.Function;
import rp.com.google.common.collect.ImmutableMap;
import rp.com.google.common.collect.Queues;

import java.util.Calendar;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.reportportal.testng.TestNGService.RP_ID;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Aspect
public class StepAspect {

	public static final String STEP_GROUP = "\\{([\\w$]+(\\.[\\w$]+)*)}";

	public static final Logger LOGGER = LoggerFactory.getLogger(StepAspect.class);

	private static InheritableThreadLocal<ITestNGService> testNgService = new InheritableThreadLocal<ITestNGService>() {
		@Override
		protected ITestNGService initialValue() {
			return ReportPortalTestNGListener.SERVICE.get();
		}
	};

	private static InheritableThreadLocal<Deque<Maybe<Long>>> stepAttributes = new InheritableThreadLocal<Deque<Maybe<Long>>>() {
		@Override
		protected Deque<Maybe<Long>> initialValue() {
			return Queues.newArrayDeque();
		}
	};

	private static InheritableThreadLocal<IAttributes> attributes = new InheritableThreadLocal<IAttributes>();

	@Pointcut("@annotation(step)")
	public void withStepAnnotation(Step step) {

	}

	@Pointcut("execution(* *.*(..))")
	public void anyMethod() {

	}

	@Before(value = "anyMethod() && withStepAnnotation(step)", argNames = "joinPoint,step")
	public void startNestedStep(JoinPoint joinPoint, Step step) {
		if (!step.isIgnored()) {

			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			String nameTemplate = step.value();
			Matcher matcher = Pattern.compile(STEP_GROUP).matcher(nameTemplate);
			Map<String, Object> parametersMap = createParamsMapping(step.templateConfig(), signature, joinPoint.getArgs());

			StringBuffer stringBuffer = new StringBuffer();
			while (matcher.find()) {
				String templatePart = matcher.group(1);
				String replacement = getReplacement(templatePart, parametersMap, step.templateConfig());
				matcher.appendReplacement(stringBuffer, replacement != null ? replacement : matcher.group());
			}
			matcher.appendTail(stringBuffer);

			Maybe<Long> parentId = stepAttributes.get().peek();
			Maybe<Long> stepMaybe;
			if (parentId != null) {
				stepMaybe = testNgService.get().startStep(stringBuffer.toString(), Calendar.getInstance().getTime(), parentId);

			} else {
				stepMaybe = testNgService.get()
						.startStep(stringBuffer.toString(),
								Calendar.getInstance().getTime(),
								(Maybe<Long>) attributes.get().getAttribute(RP_ID)
						);
			}

			stepAttributes.get().push(stepMaybe);

		}

	}

	@AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
	public void finishStep(Step step) {
		Maybe<Long> stepId = stepAttributes.get().poll();
		testNgService.get().finishStep(Statuses.PASSED, Calendar.getInstance().getTime(), stepId);
	}

	@AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
	public void failedStep(Step step, final Throwable throwable) {

		testNgService.get().sendReportPortalMsg(new Function<Long, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(Long itemId) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setTestItemId(itemId);
				rq.setLevel("ERROR");
				rq.setLogTime(Calendar.getInstance().getTime());
				if (throwable != null) {
					rq.setMessage(getStackTraceAsString(throwable));
				} else {
					rq.setMessage("Test has failed without exception");
				}
				rq.setLogTime(Calendar.getInstance().getTime());

				return rq;
			}
		});

		Maybe<Long> stepId = stepAttributes.get().poll();
		testNgService.get().finishStep(Statuses.FAILED, Calendar.getInstance().getTime(), stepId);

	}

	private Map<String, Object> createParamsMapping(StepTemplateConfig templateConfig, MethodSignature signature, final Object... args) {
		int paramsCount = Math.max(signature.getParameterNames().length, args.length);
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		builder.put(templateConfig.methodNameTemplate(), signature.getMethod().getName());
		for (int i = 0; i < paramsCount; i++) {
			builder.put(signature.getParameterNames()[i], args[i]);
			builder.put(Integer.toString(i), args[i]);
		}
		return builder.build();
	}

	@Nullable
	private String getReplacement(String templatePart, Map<String, Object> parametersMap, StepTemplateConfig templateConfig) {
		String[] fields = templatePart.split("\\.");
		String variableName = fields[0];
		Object param = parametersMap.get(variableName);
		if (param == null) {
			LOGGER.error("Param - " + variableName + " was not found");
			return null;
		}
		return StepTemplateUtils.retrieveValue(templateConfig, 1, fields, param);
	}

	public static ITestNGService getTestNgService() {
		return testNgService.get();
	}

	public static void setTestNgService(ITestNGService service) {
		testNgService.set(service);
	}

	public static IAttributes getAttributes() {
		return attributes.get();
	}

	public static void setAttributes(IAttributes iAttributes) {
		attributes.set(iAttributes);
	}
}
