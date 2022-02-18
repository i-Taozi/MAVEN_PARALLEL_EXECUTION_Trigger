/*
 * Generated by Robotoworks Mechanoid
 */
package com.robotoworks.example.movies.ops;

import com.robotoworks.mechanoid.Mechanoid;
import com.robotoworks.mechanoid.ops.Operation;
import com.robotoworks.mechanoid.ops.OperationContext;
import com.robotoworks.mechanoid.ops.OperationResult;
import com.robotoworks.mechanoid.ops.OperationServiceBridge;
import com.robotoworks.mechanoid.ops.OperationConfiguration;
import android.content.Intent;
import android.os.Bundle;

public abstract class AbstractGetMoviesOperation extends Operation {
	public static final String ACTION_GET_MOVIES = "com.robotoworks.example.movies.ops.MoviesService.actions.GET_MOVIES";


	static class Args {
	}
	
	static class Configuration extends OperationConfiguration {
		@Override 
		public Operation createOperation() {
			return new GetMoviesOperation();
		}
		
		@Override
		public Intent findMatchOnConstraint(OperationServiceBridge bridge, Intent intent) {
			Intent existingRequest = bridge.findPendingRequestByActionWithExtras(AbstractGetMoviesOperation.ACTION_GET_MOVIES, intent.getExtras());
			
			return existingRequest;
			
		}
	}
	
	public static final Intent newIntent() {
		Intent intent = new Intent(ACTION_GET_MOVIES);
		intent.setClass(Mechanoid.getApplicationContext(), MoviesService.class);
		
		Bundle extras = new Bundle();
		
		intent.putExtras(extras);
		
		return intent;
		
	}

	@Override
	public OperationResult execute(OperationContext context) {
		Args args = new Args();
		
		return onExecute(context, args);
	}
			
	protected abstract OperationResult onExecute(OperationContext context, Args args);
}
