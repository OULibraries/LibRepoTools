/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(document).ready(function(){
    
    $(".loading-btn").click(function(){
        $(".spining-class").show();
//        $(this).attr("disabled", true);
    });
    
    $('form').submit(function() {
        $(".loading-btn").attr("disabled", true);
    })
    
    $(".btn-click").click(function(){
        $(".click-show-slow").toggle("slow", function(){});
    });
    
    $(".click-show-slow").hide();
    
    $(".new-server-btn").click(function(){
        $("input[class='form-control']").val("");
        $("input[name='serverId']").val("-1");
    });
});

