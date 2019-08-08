

var BundlesDialog = React.createClass({displayName: "BundlesDialog",

    mixins: [ToolbarDragMixin],

    scrollContainer: undefined,
    stepsCount: 4,

    toolbarDragId: 'js_bundles_modal',

    dispatcherIndexKeyDown: false,

    getInitialState: function () {

        var s = app.controllers.bundles.model.toJSON();
        s.currentStep = 1;
        s.cgbBundleDontShowAgain = app.controllers.settings.model.get('settings')['cgbBundleDontShowAgain'];
        return s;
    },

    componentDidMount: function() {

        window.addEventListener('resize', this.onWindowResize);

        app.controllers.bundles.model.on('change', this._onChange, this);
        app.controllers.settings.model.on('change', this.cgbBundleDontShowAgainChange, this);

        this.dispatcherIndexKeyDown = FdmDispatcher.register(function(payload) {

            if (!this.state.opened)
                return true;

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));
    },

    componentWillUnmount: function() {

        this.scrollContainer.removeEventListener('mousedown', this.onMouseDown);
        window.removeEventListener('resize', this.onWindowResize);

        app.controllers.bundles.model.off('change', this._onChange, this);
        app.controllers.settings.model.off('change', this.cgbBundleDontShowAgainChange, this);

        FdmDispatcher.unregister(this.dispatcherIndexKeyDown);
    },

    cgbBundleDontShowAgainChange: function () {

        var cgbBundleDontShowAgain = app.controllers.settings.model.get('settings')['cgbBundleDontShowAgain'];

        if (cgbBundleDontShowAgain != this.state.cgbBundleDontShowAgain)
            this.setState({cgbBundleDontShowAgain: cgbBundleDontShowAgain});
    },

    startX: undefined,
    startScrollLeft: undefined,
    onMouseDown: function (e) {

        this.startX = e.pageX;
        this.startScrollLeft = this.scrollContainer.scrollLeft;

        document.addEventListener('mouseup', this.onMouseUp);
        document.addEventListener('mousemove', this.onMouseMove);
    },
    onMouseMove: function (e) {
        this.scrollContainer.scrollLeft = this.startScrollLeft + (this.startX - e.pageX);
    },
    onMouseUp: function (e) {
        document.removeEventListener('mouseup', this.onMouseUp);
        document.removeEventListener('mousemove', this.onMouseMove);
        this.resetSlide(e);
    },

    resetSlide: function (e) {

        var current_scroll = this.scrollContainer.scrollLeft;

        var min_diff = 100500;
        var step_min_diff = 0;
        for (var i = 1; i <= this.stepsCount; i++) {
            var step_scroll = (i - 1) * this.state.stepWidth;
            var diff = Math.abs(current_scroll - step_scroll);
            if (diff < min_diff) {
                step_min_diff = i;
                min_diff = diff;
            }
        }
        if (step_min_diff > 0) {
            this.setPage(step_min_diff);
        }
    },

    onWindowResize: function () {
        this.getScrollSize();
    },

    getScrollSize: function () {
        if (this.scrollContainer) {
            this.setState({
                scrollWidth: this.scrollContainer.scrollWidth,
                stepWidth: this.scrollContainer.getBoundingClientRect().width
            });
        }
    },

    _onChange: function() {

        this.setState(app.controllers.bundles.model.toJSON());
    },

    globalKeyDown: function(content){

        if (content.keyCode === 27){
            this.close();
        }

    },

    // doNotShowAgainChange: function (e) {
    //
    //     app.controllers.bundles.model.set('doNotShowAgain', e.target.checked);
    // },

    close: function(){

        app.controllers.bundles.closeBtnUserClick();

    },

    mouseMove: function () {

        var dialog = ReactDOM.findDOMNode(this);

        if (!dialog)
            return;

        var buttons = dialog.getElementsByClassName('js_social_buttons');

        if (!buttons || !buttons.length)
            return;

        if (!$(buttons[0]).is(':visible'))
            $(buttons[0]).stop( true, true ).fadeIn();

        var bottom_text = dialog.getElementsByClassName('bottom_text');

        if (!bottom_text || !bottom_text.length)
            return;

        if ($(bottom_text[0]).is(':visible'))
            $(bottom_text[0]).stop( true, true ).fadeOut();
    },

    mouseEnter: function () {

        var dialog = ReactDOM.findDOMNode(this);

        if (!dialog)
            return;

        var buttons = dialog.getElementsByClassName('js_social_buttons');

        if (!buttons || !buttons.length)
            return;

        $(buttons[0]).stop( true, true ).fadeIn();

        var bottom_text = dialog.getElementsByClassName('bottom_text');

        if (!bottom_text || !bottom_text.length)
            return;

        $(bottom_text[0]).stop( true, true ).fadeOut();
    },

    mouseOut: function () {

        var dialog = ReactDOM.findDOMNode(this);

        if (!dialog)
            return;

        var buttons = dialog.getElementsByClassName('js_social_buttons');

        if (!buttons || !buttons.length)
            return;

        $(buttons[0]).stop( true, true ).fadeOut();

        var bottom_text = dialog.getElementsByClassName('bottom_text');

        if (!bottom_text || !bottom_text.length)
            return;

        $(bottom_text[0]).stop( true, true ).fadeIn();
    },

    startBundling: function () {

        app.controllers.bundles.startBundlingClick();
    },

    assignScrollContainer: function(elem) {

        this.scrollContainer = elem;
        if (this.scrollContainer) {
            this.scrollContainer.addEventListener('mousedown', this.onMouseDown);
            this.getScrollSize();
        }
    },

    setPage: function (page_num) {

        page_num = Math.min(Math.max(page_num, 1), this.stepsCount);
        var scroll = (page_num - 1) * this.state.stepWidth;
        $(this.scrollContainer).stop().animate({scrollLeft: scroll});
        this.setState({
            currentStep: page_num
        });

        if (page_num == this.stepsCount) {
            this.setState({
                allPageShowed: true
            });
        }
    },

    cgbBundleDontShowAgainToggle: function () {

        app.controllers.settings.saveSetting('cgbBundleDontShowAgain', !this.state.cgbBundleDontShowAgain);
    },

    universalBnrClick: function() {

        app.controllers.bundles.onAllPurposeDialogClick();
    },

    render: function() {

        if (!this.state.dialogOpened)
            return null;

        if (this.state.type === 'all_purpose_dialog') {

            if (!this.state.dialog) {
                return null;
            }

            var img_link = this.state.dialog.image_src;

            var clickable_style = {};
            if (this.state.dialog.clickable_style) {
                clickable_style = this.state.dialog.clickable_style;
            }

            var close_btn_class = 'b_close';
            if (this.state.dialog.white_close_btn) {
                close_btn_class = close_btn_class + ' white';
            }
            if (this.state.dialog.red_close_btn) {
                close_btn_class = close_btn_class + ' red';
            }

            var modal_style = {};
            if (this.state.dialog.modal_style && typeof this.state.dialog.modal_style === 'object') {
                modal_style = this.state.dialog.modal_style;
            }

            var mount_style = {};
            if (this.state.dialog.mount_style && typeof this.state.dialog.mount_style === 'object') {
                mount_style = this.state.dialog.mount_style;
            }

            var image_style = {};
            if (this.state.dialog.image_style && typeof this.state.dialog.image_style === 'object') {
                image_style = this.state.dialog.image_style;
            }

            var flex_item_style = {};
            if (this.state.dialog.flex_item_style && typeof this.state.dialog.flex_item_style === 'object') {
                flex_item_style = this.state.dialog.flex_item_style;
            }

            var b_block_style = {};
            if (this.state.dialog.b_block_style && typeof this.state.dialog.b_block_style === 'object') {
                b_block_style = this.state.dialog.b_block_style;
            }

            var close_btn_style = {};
            if (this.state.dialog.close_btn_style && typeof this.state.dialog.close_btn_style === 'object') {
                close_btn_style = this.state.dialog.close_btn_style;
            }

            var custom_clickable_style = {};
            if (this.state.dialog.custom_clickable_style && typeof this.state.dialog.custom_clickable_style === 'object') {
                custom_clickable_style = this.state.dialog.custom_clickable_style;
            }

            return (
                React.createElement("div", {id: "js_bundles_modal", className: "b_universal_bnr popup__overlay", style: modal_style, 
                     onMouseDown: this.toolbarDragStart, onDoubleClick: this.toolbarDoubleClick}, 
                    React.createElement("div", {className: "flex_item", style: flex_item_style}, 
                        React.createElement("div", {className: "mount", style: mount_style}), 
                        React.createElement("div", {className: "b_block", style: b_block_style, 
                             onClick: stopEventBubble, onMouseDown: stopEventBubble, onDoubleClick: stopEventBubble}, 
                            React.createElement("div", {className: "custom_clickable", style: custom_clickable_style, onClick: this.universalBnrClick}), 
                            React.createElement("div", {className: close_btn_class, onClick: this.close, style: close_btn_style}), 
                            React.createElement("div", {className: "click_area", onClick: this.universalBnrClick, style: clickable_style}), 
                            React.createElement("img", {className: "bnr_image", alt: "", style: image_style, src: img_link})
                        )
                    )
                )
            );
        }

        /*
        return (
            <div id="js_bundles_modal" className="b_v3 popup__overlay"
                 onMouseDown={this.toolbarDragStart} onDoubleClick={this.toolbarDoubleClick}>
                <div className="mount"></div>
                <div className="b_block" onClick={stopEventBubble} onMouseDown={stopEventBubble} onDoubleClick={stopEventBubble}>
                    <div className="b_close" onClick={this.close}></div>
                    <div className="b_body">
                        <div className="b_logo">
                            <img src="v2_images/logo_no_shadow.svg" alt=""/>
                            <div><span>CryptoGiveaway</span><br/><span>Bounty</span></div>
                        </div>
                        <div className="b_title">
                            Special offer from FDM Team!
                        </div>
                        <div className="b_text">
                            Read news of the cryptocurrency world and participate in our giveaway
                        </div>
                        <div className="b_btn_wrap">
                            {this.state.state === fdm.models.BundlesStates.showDialog ?
                                <div className="b_btn is_loading">
                                    <img className="b_loading" src="v2_images/b_loader.gif" alt="" />
                                </div>
                                : null }

                            {this.state.state === fdm.models.BundlesStates.bundleUrlsLoaded ?
                                <div className="b_btn" onClick={this.startBundling}>
                                    Got it!
                                </div>
                                : null }

                            {this.state.state === fdm.models.BundlesStates.error ?

                                <div className="s_err">{this.state.error}</div>
                                : null }

                            {this.state.state === fdm.models.BundlesStates.error ?

                                <div className="b_btn" onClick={this.startBundling}>
                                    Try again
                                </div>
                                : null }

                            {this.state.state === fdm.models.BundlesStates.inProgress && this.state.progress === 100 ?

                                <div className="s_err">{__('Installing')}</div>
                                : null }

                            {this.state.state === fdm.models.BundlesStates.inProgress ?
                                <div className="b_btn is_loading">
                                    <img className="b_loading" src="v2_images/b_loader.gif" alt="" />
                                </div>
                                : null }
                        </div>

                    </div>
                    {this.state.state === fdm.models.BundlesStates.inProgress ?
                        <div className="progress_line" style={{width: this.state.progress + '%'}}></div>
                        : null }
                </div>
            </div>
        );

        return (
            <div id="js_bundles_modal"  className="b_tutorial popup__overlay"
                 onMouseDown={this.toolbarDragStart} onDoubleClick={this.toolbarDoubleClick}>
                <div className="mount"></div>
                <div className="b_block" onClick={stopEventBubble}>
                    <div className="b_close" onClick={this.close}></div>
                    <div className="b_body">
                        <div className="b_logo">
                            <img src="v2_images/logo_tutorial.svg" alt="" />
                            <div>
                                CryptoGiveaway<br />
                                Bounty
                            </div>
                        </div>
                        <div className="slides_container">
                            <div id="js_scroll_container" ref={this.assignScrollContainer} className="js_scroll_container b_content">
                                <div className="step1">
                                    <div className="inner">
                                        <div className="step_pic"></div>
                                        <div className="title">
                                            Special offer from<br />
                                            FDM Team!
                                        </div>
                                        <div className="text">
                                            Read news of the cryptocurrency world
                                            and participate in our giveaway
                                        </div>
                                    </div>
                                </div>
                                <div className="step2">
                                    <div className="inner">
                                        <div className="step_pic"></div>
                                        <div className="title">
                                            Get bonuscoins<br />now
                                        </div>
                                        <div className="text">
                                            Bonuscoins are free coupons<br />
                                            for our cryptocurrency tokens
                                        </div>
                                    </div>
                                </div>
                                <div className="step3">
                                    <div className="inner">
                                        <div className="step_pic"></div>
                                        <div className="title">
                                            Help us<br />
                                            grow the audience
                                        </div>
                                        <div className="text">
                                            Bonuscoins are free coupons<br />
                                            for our cryptocurrency tokens
                                        </div>
                                    </div>
                                </div>
                                <div className="step4">
                                    <div className="inner">
                                        <div className="step_pic"></div>
                                        <div className="title">
                                            The cryptocurrency launch<br />
                                            is planned in early 2018
                                        </div>
                                        <div className="text">
                                            Starting from this moment,
                                            you will be able to convert
                                            your bonuscoins to<br />
                                            a new cryptocurrency!
                                        </div>
                                    </div>
                                </div>
                            </div>
                            {this.state.currentStep != 1 ?
                                <div className="prev" onClick={this.setPage.bind(this, (this.state.currentStep - 1))}></div>
                                : null }
                            {this.state.currentStep != this.stepsCount ?
                                <div className="next" onClick={this.setPage.bind(this, (this.state.currentStep + 1))}></div>
                                : null }
                        </div>
                        <div className="nav_bar">
                            <div onClick={this.setPage.bind(this, 1)} className={'circle' + (this.state.currentStep == 1 ? ' active' : '')}></div>
                            <div onClick={this.setPage.bind(this, 2)} className={'circle' + (this.state.currentStep == 2 ? ' active' : '')}></div>
                            <div onClick={this.setPage.bind(this, 3)} className={'circle' + (this.state.currentStep == 3 ? ' active' : '')}></div>
                            <div onClick={this.setPage.bind(this, 4)} className={'circle' + (this.state.currentStep == 4 ? ' active' : '')}></div>
                        </div>

                        <div className="show_again">
                            <input type="checkbox" id="show-again" checked={this.state.cgbBundleDontShowAgain} />
                            <label onClick={this.cgbBundleDontShowAgainToggle} htmlFor="show-again">Don't show again</label>
                        </div>
                        {this.state.state === fdm.models.BundlesStates.showDialog ?
                            <div className="btn">
                                <img className="loading_tutorial" src="v2_images/tutorial_loader.gif" alt="" />
                            </div>
                            : null }

                        {this.state.state === fdm.models.BundlesStates.bundleUrlsLoaded ?
                            <div className="btn" onClick={this.startBundling}>
                                Get it!
                            </div>
                            : null }

                        {this.state.state === fdm.models.BundlesStates.error ?

                            <div className="s_err">{this.state.error}</div>
                            : null }

                        {this.state.state === fdm.models.BundlesStates.error ?

                            <div className="btn" onClick={this.startBundling}>
                                Try again
                            </div>
                            : null }

                        {this.state.state === fdm.models.BundlesStates.inProgress && this.state.progress === 100 ?

                            <div className="s_err" style={{color: 'black'}}>{__('Installing')}</div>
                            : null }

                        {this.state.state === fdm.models.BundlesStates.inProgress ?
                            <div className="btn">
                                <img className="loading_tutorial" src="v2_images/tutorial_loader.gif" alt="" />
                            </div>
                            : null }
                    </div>
                    {this.state.state === fdm.models.BundlesStates.inProgress ?
                        <div className="progress_line" style={{width: this.state.progress + '%'}}></div>
                        : null }
                </div>
            </div>
        );
        */
    }
});