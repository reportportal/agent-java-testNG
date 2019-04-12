package com.epam.reportportal.testng.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import com.epam.reportportal.testng.ITestNGService;
import com.epam.reportportal.testng.ReportPortalTestNGListener;
import com.epam.reportportal.utils.StepTemplateUtils;
import io.reactivex.annotations.Nullable;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import rp.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static InheritableThreadLocal<IAttributes> attributes = new InheritableThreadLocal<IAttributes>();

	//	@Pointcut("@annotation(com.epam.reportportal.annotations.Step)")
	//	public void withNestedStepAnnotation() {
	//
	//	}

	@Pointcut("@annotation(step)")
	public void withNestedStepAnnotation(Step step) {

	}

	@Pointcut("execution(* *.*(..))")
	public void anyMethod() {

	}

	@Before(value = "anyMethod() && withNestedStepAnnotation(step)", argNames = "joinPoint,step")
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

			testNgService.get().startStep(stringBuffer.toString(), attributes.get());

		}

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
