).
       *                                For a 7 pointed star, we might do
       *                                360/7 * 3 = 154.286)
       *
       *      SHRINK_TIME = 400ms
       */

      .qp-circular-loader {
        width:28px;  /* 2*RADIUS + STROKEWIDTH */
        height:28px; /* 2*RADIUS + STROKEWIDTH */
      }
      .qp-circular-loader-path {
        stroke-dasharray: 58.9;  /* 2*RADIUS*PI * ARCSIZE/360 */
        stroke-dashoffset: 58.9; /* 2*RADIUS*PI * ARCSIZE/360 */
                                 /* hides things initially */
      }

      /* SVG elements seem to have a different default origin */
      .qp-circular-loader, .qp-circular-loader * {
        -webkit-transform-origin: 50% 50%;
      }

      /* Rotating the whole thing */
      @-webkit-keyframes rotate {
        from {transform: rotate(0deg);}
        to {transform: rotate(360deg);}
      }
      .qp-circular-loader {
        -webkit-animation-name: rotate;
        -webkit-animation-duration: 1568.63ms; /* 360 * ARCTIME / (ARCSTARTROT + (360-ARCSIZE)) */
        -webkit-animation-iteration-count: infinite;
        -webkit-animation-timing-function: linear;
      }

      /* Filling and unfilling the arc */
      @-webkit-keyframes fillunfill {
        from {
          stroke-dashoffset: 58.8 /* 2*RADIUS*PI * ARCSIZE/360 - 0.1 */
                                  /* 0.1 a bit of a magic constant here */
        }
        50% {
          stroke-dashoffset: 0;
        }
        to {
          stroke-dashoffset: -58.4 /* -(2*RADIUS*PI * ARCSIZE/360 - 0.5) */
                                   /* 0.5 a bit of a magic constant here */
        }
      }
      @-webkit-keyframes rot {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(-360deg);
        }
      }
      @-webkit-keyframes colors {
        from {
          stroke: #4285f4;
        }
        to {
          stroke: #4285f4;
        }
      }
      .qp-circular-loader-path {
        -webkit-animation-name: fillunfill, rot, colors;
        -webkit-animation-duration: 1333ms, 5332ms, 5332ms; /* ARCTIME, 4*ARCTIME, 4*ARCTIME */
        -webkit-animation-iteration-count: infinite, infinite, infinite;
        -webkit-animation-timing-function: cubic-bezier(0.4, 0.0, 0.2, 1), steps(4), linear;
        -webkit-animation-play-state: running, running, running;
        -webkit-animation-fill-mode: forwards;
      }

  </style>

  <!-- 3= STROKEWIDTH -->
  <!-- 14= RADIUS + STROKEWIDTH/2 -->
  <!-- 12.5= RADIUS -->
  <!-- 1.5=  STROKEWIDTH/2 -->
  <!-- ARCSIZE would affect the 1.5,14 part of this... 1.5,14 is specific to
       270 degress -->
  <g class="qp-circular-loader">
    <path class="qp-circular-loader-path" fill="none" 
          d="M 14,1.5 A 12.5,12.5 0 1 1 1.5,14" stroke-width="3"
          stroke-linecap="round"></path>
  </g>
</svg>
<!-- This version of the throbber is good for sizes less than 28x28dp,
     which follow the stroke thickness calculation: 3 - (28 - diameter) / 16 -->
<svg version="1" xmlns="http://www.w3.org/2000/svg"
                 xmlns:xlink="http://www.w3.org/1999/xlink"
     width="16px" height="16px" viewBox="0 0 16 16">
  <!-- 16= RADIUS*2 + STROKEWIDTH -->

  <title>Material design circular activity spinner with CSS3 animation</title>
  <style type="text/css">
      /**************************/
      /* STYLES FOR THE SPINNER */
      /**************************/

      /*
       * Constants:
       *      RADIUS      = 6.875
       *      STROKEWIDTH = 2.25
       *      ARCSIZE     = 270 degrees (amount of circle the arc takes up)
       *      ARCTIME     = 1333ms (time it takes to expand and contract arc)
       *      ARCSTARTROT = 216 degrees (how much the start location of the arc
       *                                should rotate each time, 216 gives us a
       *                                5 pointed star shape (it's 360/5 * 2).
       *                                For a 7 pointed star, we might do
       *                                360/7 * 3 = 154.286)
       *
       *      SHRINK_TIME = 400ms
       */

      .qp-circular-loader {
        width:16px;  /* 2*RADIUS + STROKEWIDTH */
        height:16px; /* 2*RADIUS + STROKEWIDTH */
      }
      .qp-circular-loader-path {
        stroke-dasharray: 32.4;  /* 2*RADIUS*PI * ARCSIZE/360 */
        stroke-dashoffset: 32.4; /* 2*RADIUS*PI * ARCSIZE/360 */
                                 /* hides things initially */
      }

      /* SVG elements seem to have a different default origin */
      .qp-circular-loader, .qp-circular-loader * {
        -webkit-transform-origin: 50% 50%;
      }

      /* Rotating the whole thing */
      @-webkit-keyframes rotate {
        from {transform: rotate(0deg);}
        to {transform: rotate(360deg);}
      }
      .qp-circular-loader {
        -webkit-animation-name: rotate;
        -webkit-animation-duration: 1568.63ms; /* 360 * ARCTIME / (ARCSTARTROT + (360-ARCSIZE)) */
        -webkit-animation-iteration-count: infinite;
        -webkit-animation-timing-function: linear;
      }

      /* Filling and unfilling the arc */
      @-webkit-keyframes fillunfill {
        from {
          stroke-dashoffset: 32.3 /* 2*RADIUS*PI * ARCSIZE/360 - 0.1 */
                                  /* 0.1 a bit of a magic constant here */
        }
        50% {
          stroke-dashoffset: 0;
        }
        to {
          stroke-dashoffset: -31.9 /* -(2*RADIUS*PI * ARCSIZE/360 - 0.5) */
                                   /* 0.5 a bit of a magic constant here */
        }
      }
      @-webkit-keyframes rot {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(-360deg);
        }
      }
      @-webkit-keyframes colors {
        from {
          stroke: #4285f4;
        }
        to {
          stroke: #4285f4;
        }
      }
      .qp-circular-loader-path {
        -webkit-animation-name: fillunfill, rot, colors;
        -webkit-animation-duration: 1333ms, 5332ms, 5332ms; /* ARCTIME, 4*ARCTIME, 4*ARCTIME */
        -webkit-animation-iteration-count: infinite, infinite, infinite;
        -webkit-animation-timing-function: cubic-bezier(0.4, 0.0, 0.2, 1), steps(4), linear;
        -webkit-animation-play-state: running, running, running;
        -webkit-animation-fill-mode: forwards;
      }

  </style>

  <!-- 2.25= STROKEWIDTH -->
  <!-- 8 = RADIUS + STROKEWIDTH/2 -->
  <!-- 6.875= RADIUS -->
  <!-- 1.125=  STROKEWIDTH/2 -->
  <g class="qp-circular-loader">
    <path class="qp-circular-loader-path" fill="none" 
          d="M 8,1.125 A 6.875,6.875 0 1 1 1.125,8" stroke-width="2.25"
          stroke-linecap="round"></path>
  </g>
</svg>
�PNG
