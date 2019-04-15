package com.epam.reportportal.testng.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import com.epam.reportportal.annotations.UniqueID;
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
import org.testng.annotations.Test;
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

	private static InheritableThreadLocal<Deque<Maybe<Long>>> stepStack = new InheritableThreadLocal<Deque<Maybe<Long>>>() {
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
			Test testAnnotation = signature.getMethod().getAnnotation(Test.class);
			if (testAnnotation != null) {
				LOGGER.error("Method annotated with '@Step' cannot be annotated with '@Test'");
				return;
			}

			Maybe<Long> parentId = stepStack.get().peek();
			if (parentId == null) {
				parentId = (Maybe<Long>) attributes.get().getAttribute(RP_ID);
			}

			UniqueID uniqueIdAnnotation = signature.getMethod().getAnnotation(UniqueID.class);
			String uniqueId = uniqueIdAnnotation != null ? uniqueIdAnnotation.value() : null;
			String name = getStepName(step, signature, joinPoint);

			Maybe<Long> stepMaybe = testNgService.get()
					.startNestedStep(uniqueId, name, step.description(), Calendar.getInstance().getTime(), parentId);
			stepStack.get().push(stepMaybe);

		}

	}

	@AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
	public void finishNestedStep(Step step) {
		if (!step.isIgnored()) {
			Maybe<Long> stepId = stepStack.get().poll();
			if (stepId == null) {
				LOGGER.error("Id of the 'STEP' to finish retrieved from step stack is NULL");
				return;
			}
			testNgService.get().finishNestedStep(Statuses.PASSED, Calendar.getInstance().getTime(), stepId);
		}
	}

	@AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
	public void failedNestedStep(Step step, final Throwable throwable) {

		if (!step.isIgnored()) {

			Maybe<Long> stepId = stepStack.get().poll();
			if (stepId == null) {
				LOGGER.error("Id of the 'STEP' to finish retrieved from step stack is NULL");
				return;
			}

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

			testNgService.get().finishNestedStep(Statuses.FAILED, Calendar.getInstance().getTime(), stepId);
		}

	}

	private String getStepName(Step step, MethodSignature signature, JoinPoint joinPoint) {
		String nameTemplate = step.value();
		if (nameTemplate.trim().isEmpty()) {
			return signature.getMethod().getName();
		}
		Matcher matcher = Pattern.compile(STEP_GROUP).matcher(nameTemplate);
		Map<String, Object> parametersMap = createParamsMapping(step.templateConfig(), signature, joinPoint.getArgs());

		StringBuffer stringBuffer = new StringBuffer();
		while (matcher.find()) {
			String templatePart = matcher.group(1);
			String replacement = getReplacement(templatePart, parametersMap, step.templateConfig());
			matcher.appendReplacement(stringBuffer, replacement != null ? replacement : matcher.group());
		}
		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
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
