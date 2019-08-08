jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");


fdm.models.UpdaterManagers = {
	Client: 0
};

fdm.models.UpdaterStages = {
	check_updates: 0,
	download_updates: 1,
	post_download_check: 2,
	pre_install_check: 3,
	install_updates: 4
};

fdm.models.UpdaterStates = {
	ready: 0,
	in_progress: 1,
	failed: 2,
	finished: 3
};

fdm.models.UpdaterRestartType = {
	none: 0,
	module: 1,
	os: 2
};

fdm.models.UpdateInitiator = {
	unknown: 0,
	automatic: 1,
	user: 2
};

fdm.controllers.Updater = (function () {
	function Class(apiUpdater) {

		_.bindAll(this, 'onItemChanged');

		fdm.models.Updater = Backbone.Model.extend({
			defaults: {
				//id: fdm.models.UpdaterManagers.Client
				//hasUpdate: false
				dialogShown: false,
				updateButtonShown: false,
				stage: fdm.models.UpdaterStages.check_updates,
				state: fdm.models.UpdaterStates.ready,
				progress: {current:0, max:0},
				lastFinishedCheckForUpdates: 0,
				preDownloadCheck: 0,
				initiator: 0,
				lastDownloadClick: 0
			},
			initialize: function(){

				this.onItemChanged();
			},

			onItemChanged: function(){

				var state = {};

				state.getUpdateTargetInfo = this.updateTargetInfo();
				state.getLastErrorDescription = this.getLastErrorDescription();
				state.updatesAvailable = this.updatesAvailable();
				state.stage = this.updateStage();
				state.state = this.updateState();
				state.progress = this.updateProgress();
				state.getPreInstallCheckFailureReason = this.getPreInstallCheckFailureReason();
				state.getRestartRequired = this.getRestartRequired();
				state.initiator = this.getInitiator();

				this.set(state);
			},

			updateTargetInfo: function(){
				return apiUpdater.getUpdateTargetInfo(this.get('id'))
			},
			getLastErrorDescription: function(){
				return apiUpdater.getLastErrorDescription(this.get('id'))
			},
			updatesAvailable: function(){
				return apiUpdater.updatesAvailable(this.get('id'))
			},
			updateStage: function(){
				return apiUpdater.getStage(this.get('id'))
			},
			updateState: function(){
				return apiUpdater.getState(this.get('id'))
			},
			updateProgress: function(){
				return apiUpdater.getProgress(this.get('id'))
			},

			getPreInstallCheckFailureReason: function(){
				return apiUpdater.getPreInstallCheckFailureReason(this.get('id'))
			},
			shutdownUpdateTargetModule: function(){
				apiUpdater.shutdownUpdateTargetModule(this.get('id'), 0, _.bind(this.shutdownUpdateTargetModuleCallback, this))
			},
			shutdownUpdateTargetModuleCallback: function(args){
				this.set({shutdownUpdateTargetModule: args});
			},
			getRestartRequired: function(){
				return apiUpdater.getRestartRequired(this.get('id'))
			},

			checkForUpdates: function(){
				apiUpdater.checkForUpdates(this.get('id'));
				this.set({
					dialogShown: true,
					lastFinishedCheckForUpdates: 0
				});
			},
			downloadUpdates: function(force_download){

				force_download = force_download || false;

				var f_time = this.get('lastFinishedCheckForUpdates');

				if (!force_download && (!f_time || (+ new Date() ) - f_time > 5 * 60 * 1000)){

					this.set({preDownloadCheck: (+ new Date() )});
					apiUpdater.checkForUpdates(this.get('id'));
				}
				else{

					this.set({
						lastDownloadClick: (+ new Date() ),
						preDownloadCheck: 0
					});
					apiUpdater.downloadUpdates(this.get('id'));
				}
			},
			installUpdates: function(){
				apiUpdater.installUpdates(this.get('id'))
			},
			abortOperation: function(){
				apiUpdater.abortOperation(this.get('id'))
			},
			performRestart: function(){
				apiUpdater.performRestart(this.get('id'))
			},
			reset: function(){
				apiUpdater.reset(this.get('id'));
			},
			getInitiator: function(){
				return apiUpdater.getInitiator(this.get('id'));
			}

		});
		this.model = new fdm.models.Updater({id: fdm.models.UpdaterManagers.Client});

		this.model.on('change', this.onModelChanged, this);

		apiUpdater.onItemChanged = this.onItemChanged;
	}

	Class.prototype = {

		onItemChanged: function(id){
			if (id == fdm.models.UpdaterManagers.Client)
				this.model.onItemChanged();
		},

		finishCheckForUpdates: function(){
			this.model.set({
				lastFinishedCheckForUpdates: + new Date(),
			});
			setTimeout(_.bind(function(){
				this.model.trigger('change');
			}, this), 15000);
		},

		onModelChanged: function(model){

			if ( model && model.changed
				&& (model.changed.hasOwnProperty('stage')
				|| model.changed.hasOwnProperty('state')
				|| model.changed.hasOwnProperty('updatesAvailable')))
			{
				var json = this.model.toJSON();

				if (json.stage == fdm.models.UpdaterStages.check_updates
					&& json.state == fdm.models.UpdaterStates.finished ){

					app.controllers.adsModal.updateShowShareDialog();

					if (json.initiator == fdm.models.UpdateInitiator.user
						&& json.updatesAvailable)
					{
						if (json.preDownloadCheck && json.lastFinishedCheckForUpdates < json.preDownloadCheck
							&& (+ new Date() ) - json.preDownloadCheck < 60000){

							this.model.downloadUpdates(true);

							_.defer(function(){

								this.model.set({
									preDownloadCheck: 0,
									lastFinishedCheckForUpdates: 0
								});
							}.bind(this));

						}
						else{

							_.defer(this.finishCheckForUpdates.bind(this));
						}
					}

					if (!json.updatesAvailable)
						_.defer(this.finishCheckForUpdates.bind(this));
				}

				if (json.initiator == fdm.models.UpdateInitiator.user
					&& json.stage == fdm.models.UpdaterStages.post_download_check
					&& json.state == fdm.models.UpdaterStates.finished
					&& (+ new Date() ) - json.lastDownloadClick < 5 * 60000 ){

					this.model.installUpdates();
					this.model.set({
						lastDownloadClick: 0,
						stage: fdm.models.UpdaterStages.pre_install_check,
						state: fdm.models.UpdaterStates.ready
					});

				}
			}
		},

		/*
		onModelChanged: function(model){

			if ( model && model.changed
				&& (model.changed.stage || model.changed.state || model.changed.updatesAvailable))
			{
				var json = this.model.toJSON();

				if (json.stage == fdm.models.UpdaterStages.check_updates
					&& json.state == fdm.models.UpdaterStates.finished
					&& !json.updatesAvailable ){

					_.defer(_.bind(this.updateFinishedMTime, this));
				}
			}
		},
		*/

		checkForUpdates: function(){

			this.model.set({
				preDownloadCheck: 0,
				lastDownloadClick: 0
			});
			this.model.checkForUpdates();
		}
	};

	return Class;
})();

