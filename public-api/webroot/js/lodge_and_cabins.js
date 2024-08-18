// $( document ).ready(function() {
//   $('.carousel').carousel();
//
//   $('.image-carousel').on('slid.bs.carousel', function (event) {
//     var carouselId = event.currentTarget.id;
//     var thumbnailCarousel = $("#"+carouselId + "-thumbnails");
//     var currentIndex = $('#lodge-carousel .active').index();
//
//     var slideIndex = parseInt(currentIndex/6);
//     var slideItemIndex = currentIndex%6;
//
//     thumbnailCarousel.carousel(slideIndex);
//
//     var currentSelected = thumbnailCarousel.find(".current-image");
//     currentSelected.removeClass("current-image");
//
//     var slide = thumbnailCarousel.find(".row:eq("+slideIndex+")");
//     var slideItemParent = slide.find(".col-sm-2:eq("+slideItemIndex+")");
//
//     var thumbnail = slideItemParent.find(".thumbnail");
//     thumbnail.addClass("current-image")
//   })
//
//   var url = document.location.toString();
//
//   if (url.match('#')) {
//       $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
//   } //add a suffix
//
//   // Change hash for page-reload
//   $('.nav-tabs a').on('shown.bs.tab', function (e) {
//       window.location.hash = e.target.hash;
//   })
// });

function selectThumbnail(elem){
  var jqElem = $(elem);
  var parent = jqElem.parent();
  var slide = jqElem.closest(".carousel-item");

  var carouselInner = jqElem.closest(".carousel-inner");

  var slideIndex = slide.index();
  var myIndex = parent.index();

  var currentSelected = carouselInner.find(".current-image");
  currentSelected.removeClass("current-image");

  jqElem.addClass("current-image");

  var imageIndex = (slide.index() * 6) + parent.index();

  var carousel = jqElem.closest(".carousel");

  var mainCarousel = $("#"+carousel.attr("id").replace("-thumbnails", ""));
  mainCarousel.carousel(imageIndex);

  return false;
}
