package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;



@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=rateLimiterConfiguration")
public class RateLimiterConfigurationManager {

	private static final Logger log = LoggerFactory.getLogger(RateLimiterConfigurationManager.class);

	final LoadingCache<String, UploadProcessingConfiguration> requestConfigMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, UploadProcessingConfiguration>() {

				@Override
				public UploadProcessingConfiguration load(String arg0)
						throws Exception {
					log.trace("Created new bucket for #{}", arg0);
					return new UploadProcessingConfiguration();
				}
			});

	private UploadProcessingConfiguration masterProcessingConfiguration = new UploadProcessingConfiguration();

	// ///////////////
	// Configuration//
	// ///////////////

	/** The default request capacity. volatile because it can be changed. */
	// 1mb/s
	private volatile long defaultRatePerRequestInKiloBytes = 1024;

	// 10mb/s
	private volatile long defaultRatePerClientInKiloBytes = 10 * 1024;

	// 10mb/s
	private volatile long maximumRatePerClientInKiloBytes = 10 * 1024;

	// 10mb/s
	private volatile long maximumOverAllRateInKiloBytes = 10 * 1024;



	// ///////////////


	/**
	 * Specify that a request has to be cancelled, the file is scheduled for deletion.
	 * 
	 * @param fileId
	 * @return true if there was a pending upload for this file.
	 */
	public boolean markRequestHasShallBeCancelled(String fileId) {
		UploadProcessingConfiguration ifPresent = requestConfigMap.getIfPresent(fileId);
		// if we have a value in the map
		if (ifPresent != null) {
			UploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
			// and if this file is currently being processed
			if (unchecked.isProcessing()) {
				// we ask for cancellation
				unchecked.cancelRequest = true;
			}
			// we return true if the file was processing, false otherwise
			return unchecked.isProcessing();
		}
		// if we dont have a value in the map, there is no pending upload
		else {
			// we can return false
			return false;
		}
	}


	public boolean requestIsReset(String fileId) {
		UploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		return unchecked.cancelRequest && !unchecked.isProcessing();
	}


	public boolean requestHasToBeCancelled(String fileId) {
		UploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		return unchecked.cancelRequest;
	}


	public Set<Entry<String, UploadProcessingConfiguration>> getEntries() {
		return requestConfigMap.asMap().entrySet();
	}


	public void reset(String fileId) {
		final UploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		unchecked.cancelRequest = false;
		unchecked.setProcessing(false);
	}


	public long getAllowance(String fileId) {
		return requestConfigMap.getUnchecked(fileId).getDownloadAllowanceForIteration();
	}


	public void assignRateToRequest(String fileId, Long rateInKiloBytes) {
		requestConfigMap.getUnchecked(fileId).rateInKiloBytes = rateInKiloBytes;
	}


	public Long getUploadState(String requestIdentifier) {
		return requestConfigMap.getUnchecked(requestIdentifier).instantRateInBytes;
	}


	public UploadProcessingConfiguration getUploadProcessingConfiguration(String fileId) {
		return requestConfigMap.getUnchecked(fileId);
	}


	public void pause(String fileId) {
		requestConfigMap.getUnchecked(fileId).setPaused(true);
	}


	public void resume(String fileId) {
		requestConfigMap.getUnchecked(fileId).setPaused(false);
	}


	@ManagedAttribute
	public long getDefaultRatePerRequestInKiloBytes() {
		return defaultRatePerRequestInKiloBytes;
	}


	@ManagedAttribute
	public void setDefaultRatePerRequestInKiloBytes(long defaultRatePerRequestInKiloBytes) {
		this.defaultRatePerRequestInKiloBytes = defaultRatePerRequestInKiloBytes;
	}


	@ManagedAttribute
	public long getDefaultRatePerClientInKiloBytes() {
		return defaultRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public void setDefaultRatePerClientInKiloBytes(long defaultRatePerClientInKiloBytes) {
		this.defaultRatePerClientInKiloBytes = defaultRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public long getMaximumRatePerClientInKiloBytes() {
		return maximumRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public void setMaximumRatePerClientInKiloBytes(long maximumRatePerClientInKiloBytes) {
		this.maximumRatePerClientInKiloBytes = maximumRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public long getMaximumOverAllRateInKiloBytes() {
		return maximumOverAllRateInKiloBytes;
	}


	@ManagedAttribute
	public void setMaximumOverAllRateInKiloBytes(long maximumOverAllRateInKiloBytes) {
		this.maximumOverAllRateInKiloBytes = maximumOverAllRateInKiloBytes;
	}


	public UploadProcessingConfiguration getMasterProcessingConfiguration() {
		return masterProcessingConfiguration;
	}
}