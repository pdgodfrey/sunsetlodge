$( document ).ready(function() {
  $('.carousel').carousel();

  var url = document.location.toString();

  if (url.match('#')) {
      $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
  } //add a suffix

  // Change hash for page-reload
  $('.nav-tabs a').on('shown.bs.tab', function (e) {
      window.location.hash = e.target.hash;
  })
});


function handleBuildingChange(elem){
  console.log("on change");
  console.log(elem.value)
  console.log(  $(".building-container"))
  $(".building-container").addClass("building-hidden");
  $("#building-"+elem.value).removeClass("building-hidden");

}
