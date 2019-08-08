      )
            );
        }

        if (this.state.defaultTrtClientDialogOpened) {
            return (

                React.createElement("div", {className: "wrap_default_client"}, 
                    React.createElement("span", null, __('Would you like to make Free Download Manager the default torrent client?')), 
                    React.createElement("a", {onClick: this.notShowMessageAgain, href: "#"}, __('Don\