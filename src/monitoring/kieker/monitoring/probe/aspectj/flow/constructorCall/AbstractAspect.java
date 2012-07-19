/***************************************************************************
 * Copyright 2012 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.monitoring.probe.aspectj.flow.constructorCall;

import org.aspectj.lang.JoinPoint.EnclosingStaticPart;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import kieker.common.record.flow.trace.Trace;
import kieker.common.record.flow.trace.operation.constructor.CallConstructorEvent;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.registry.TraceRegistry;
import kieker.monitoring.probe.aspectj.AbstractAspectJProbe;
import kieker.monitoring.timer.ITimeSource;

/**
 * @author Jan Waller
 */
@Aspect
public abstract class AbstractAspect extends AbstractAspectJProbe {
	private static final IMonitoringController CTRLINST = MonitoringController.getInstance();
	private static final ITimeSource TIME = CTRLINST.getTimeSource();
	private static final TraceRegistry TRACEREGISTRY = TraceRegistry.INSTANCE;

	@Pointcut
	public abstract void monitoredConstructor();

	// TODO: the detection of the caller with EnclosingStaticPart might be wrong!

	@Around("monitoredConstructor() && this(thisObject) && notWithinKieker()")
	public Object member2constructor(final Object thisObject, final ProceedingJoinPoint thisJoinPoint,
			final EnclosingStaticPart thisEnclosingJoinPoint) throws Throwable { // NOCS
		if (!CTRLINST.isMonitoringEnabled()) {
			return thisJoinPoint.proceed();
		}
		// common fields
		Trace trace = TRACEREGISTRY.getTrace();
		final boolean newTrace = trace == null;
		if (newTrace) {
			trace = TRACEREGISTRY.registerTrace();
			CTRLINST.newMonitoringRecord(trace);
		}
		final long traceId = trace.getTraceId();
		// caller
		final String caller = thisEnclosingJoinPoint.getSignature().toLongString();
		final String callerClazz = thisObject.getClass().getName();
		// callee
		final Signature calleeSig = thisJoinPoint.getSignature();
		final String callee = calleeSig.toLongString();
		final String calleeClazz = calleeSig.getDeclaringTypeName();
		// measure before call
		CTRLINST.newMonitoringRecord(new CallConstructorEvent(TIME.getTime(), traceId, trace.getNextOrderId(),
				caller, callerClazz, callee, calleeClazz));
		// call of the called method
		final Object retval;
		try {
			retval = thisJoinPoint.proceed();
		} finally {
			if (newTrace) { // close the trace
				TRACEREGISTRY.unregisterTrace();
			}
		}
		return retval;
	}

	@Around("monitoredConstructor() && !this(java.lang.Object) && notWithinKieker()")
	public Object static2constructor(final ProceedingJoinPoint thisJoinPoint, final EnclosingStaticPart thisEnclosingJoinPoint)
			throws Throwable { // NOCS
		if (!CTRLINST.isMonitoringEnabled()) {
			return thisJoinPoint.proceed();
		}
		// common fields
		Trace trace = TRACEREGISTRY.getTrace();
		final boolean newTrace = trace == null;
		if (newTrace) {
			trace = TRACEREGISTRY.registerTrace();
			CTRLINST.newMonitoringRecord(trace);
		}
		final long traceId = trace.getTraceId();
		// caller
		final Signature callerSig = thisEnclosingJoinPoint.getSignature();
		final String caller = callerSig.toLongString();
		final String callerClazz = callerSig.getDeclaringTypeName();
		// callee
		final Signature calleeSig = thisJoinPoint.getSignature();
		final String callee = calleeSig.toLongString();
		final String calleeClazz = calleeSig.getDeclaringTypeName();
		// measure before call
		CTRLINST.newMonitoringRecord(new CallConstructorEvent(TIME.getTime(), traceId, trace.getNextOrderId(),
				caller, callerClazz, callee, calleeClazz));
		// call of the called method
		final Object retval;
		try {
			retval = thisJoinPoint.proceed();
		} finally {
			if (newTrace) { // close the trace
				TRACEREGISTRY.unregisterTrace();
			}
		}
		return retval;
	}
}