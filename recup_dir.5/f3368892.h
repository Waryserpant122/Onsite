nLabel"/>
    <!-- Wrapper is used to properly compute the search tooltip position -->
    <div>
      <button id="add" data-localization="addBtnLabel"/>
    </div>
    <button id="edit" disabled="disabled" data-localization="editBtnLabel"/>
  </div>
  <script type="application/javascript">
    "use strict";
    /* global ManageAddresses */
    new ManageAddresses({
      records: document.getElementById("addresses"),
      controlsContainer: document.getElementById("controls-container"),
      remove: document.getElementById("remove"),
      add: document.getElementById("add"),
      edit: document.getElementById("edit"),
    });
  </script>
</body>
</html>
PK