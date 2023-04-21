function selectReason(elem){
  if(elem.value == "Other Inquiry"){
    $("#other-reason-container").show();
    $("#other_reason_for_contact").attr("required", true);
  } else {
    $("#other-reason-container").hide();
    $("#other_reason_for_contact").attr("required", false);
  }
}