if (!window.NEODYMIUM) {
	function NeodymiumInteractor() {
		function getStyle(element, style) {
			var value;
			var doc = element.ownerDocument;
			if (doc && doc.defaultView && doc.defaultView.getComputedStyle) {
				var css = doc.defaultView.getComputedStyle(element, null);
				value = css ? css.getPropertyValue(style) : null;
			}
			return value === 'auto' ? null : value;
		}

		function getEffectiveStyle(element, style) {
			var effectiveStyle = getStyle(element, style);
			if ('inherit' === effectiveStyle && element.parentNode) {
				return getEffectiveStyle(element.parentNode, style);
			}
			return effectiveStyle;
		}

		function getParentElement(a) {
			for (a = a.parentNode; a && a.nodeType != 1 && a.nodeType != 9
					&& a.nodeType != 11;) {
				a = a.parentNode;
			}
			return !!a && a.nodeType === 1 ? a : null;
		}

		function getClientRect(element) {
			var box;

			if (element.nodeType === 1
					&& element.tagName.toUpperCase() === 'HTML') {
				element = element.ownerDocument;
				element = ('CSS1Compat' === element.compatMode) ? element.documentElement
						: element.body;
				return {
					left : 0,
					top : 0,
					height : element.clientHeight,
					width : element.clientWidth,
					right : element.clientWidth,
					bottom : element.clientHeight
				};
			}

			try {
				box = element.getBoundingClientRect();
			} catch (e) {
				return {
					left : 0,
					top : 0,
					right : 0,
					bottom : 0,
					height : 0,
					width : 0
				};
			}

			return {
				left : box.left,
				top : box.top,
				width : box.right - box.left,
				height : box.bottom - box.top,
				right : box.right,
				bottom : box.bottom
			};
		}

		function isDisplayed(element) {
			var display = getEffectiveStyle(element, 'display');
			if ('none' === display) {
				return false;
			}
			element = getParentElement(element);
			return !element || isDisplayed(element);
		}

		function consumesSpace(element) {
			var box = getClientRect(element), a;
			if (box.height > 0 && box.width > 0) {
				return true;
			}
			if (element.nodeType === 1
					&& element.nodeName.toUpperCase() === 'PATH'
					&& (box.height > 0 || box.width > 0)) {
				a = getEffectiveStyle(element, 'stroke-width');
				return !!a && parseInt(a, 10) > 0;
			}
			if (getEffectiveStyle(element, 'overflow') != 'hidden') {
				for (var c = element.childNodes, l = c.length, i = 0, n; i < l; i++) {
					n = c.item(i);
					if (n.nodeType === 3 || n.nodeType === 1
							&& consumesSpace(n)) {
						return true;
					}
				}
			}
			return false;
		}

		function getOverflowState(elem) {
			var region = getClientRect(elem);
			var ownerDoc = elem.ownerDocument || elem.document;
			var htmlElem = ownerDoc.documentElement;
			var bodyElem = ownerDoc.body;
			var htmlOverflowStyle = getEffectiveStyle(htmlElem, 'overflow');
			var treatAsFixedPosition;

			// Return the closest ancestor that the given element may overflow.
			function getOverflowParent(e) {
				function canBeOverflowed(container) {
					// The HTML element can always be overflowed.
					if (container == htmlElem) {
						return true;
					}
					// An element cannot overflow an element with an inline
					// display
					// style.
					var containerDisplay = getEffectiveStyle(container,
							'display');
					if (/^inline/.test(containerDisplay)) {
						return false;
					}
					// An absolute-positioned element cannot overflow a
					// static-positioned one.
					if (position === 'absolute'
							&& getEffectiveStyle(container, 'position') === 'static') {
						return false;
					}
					return true;
				}

				var position = getEffectiveStyle(e, 'position');
				if (position === 'fixed') {
					treatAsFixedPosition = true;
					// Fixed-position element may only overflow the viewport.
					return e === htmlElem ? null : htmlElem;
				} else {
					var parent = getParentElement(e);
					while (parent && !canBeOverflowed(parent)) {
						parent = getParentElement(parent);
					}
					return parent;
				}
			}

			// Return the x and y overflow styles for the given element.
			function getOverflowStyles(e) {
				// When the <html> element has an overflow style of 'visible',
				// it
				// assumes
				// the overflow style of the body, and the body is really
				// overflow:visible.
				var overflowElem = e;
				if (htmlOverflowStyle === 'visible') {
					// Note: bodyElem will be null/undefined in SVG documents.
					if (e === htmlElem && bodyElem) {
						overflowElem = bodyElem;
					} else if (e == bodyElem) {
						return {
							x : 'visible',
							y : 'visible'
						};
					}
				}

				var overflow = {
					x : getEffectiveStyle(overflowElem, 'overflow-x'),
					y : getEffectiveStyle(overflowElem, 'overflow-y')
				};

				// The <html> element cannot have a genuine 'visible' overflow
				// style,
				// because the viewport can't expand; 'visible' is really
				// 'auto'.
				if (e === htmlElem) {
					overflow.x = overflow.x === 'visible' ? 'auto' : overflow.x;
					overflow.y = overflow.y === 'visible' ? 'auto' : overflow.y;
				}

				return overflow;
			}

			// Returns the scroll offset of the given element.
			function getScroll(e) {
				if (e === htmlElem) {
					var scrollE = ownerDoc.scrollingElement || bodyElem
							|| htmlElem;
					var w = ownerDoc.parentWindow || ownerDoc.defaultView;
					return {
						x : w.pageXOffset || scrollE.scrollLeft,
						y : w.pageYOffset || scrollE.scrollTop
					}
				} else {
					return {
						x : e.scrollLeft,
						y : e.scrollTop
					};
				}
			}

			// Check if the element overflows any ancestor element.
			for (var container = getOverflowParent(elem); !!container; container = getOverflowParent(container)) {
				var containerOverflow = getOverflowStyles(container);

				// If the container has overflow:visible, the element cannot
				// overflow it.
				if (containerOverflow.x === 'visible'
						&& containerOverflow.y === 'visible') {
					continue;
				}

				var containerRect = getClientRect(container);

				// Zero-sized containers without overflow:visible hide all
				// descendants.
				if (containerRect.width === 0 || containerRect.height === 0) {
					return 'hidden';
				}

				// Check 'underflow': if an element is to the left or above the
				// container
				var underflowsX = region.right < containerRect.left;
				var underflowsY = region.bottom < containerRect.top;
				if ((underflowsX && containerOverflow.x === 'hidden')
						|| (underflowsY && containerOverflow.y === 'hidden')) {
					return 'hidden';
				}

				if ((underflowsX && containerOverflow.x !== 'visible')
						|| (underflowsY && containerOverflow.y !== 'visible')) {

					// When the element is positioned to the left or above a
					// container, we
					// have to distinguish between the element being completely
					// outside the
					// container and merely scrolled out of view within the
					// container.
					var containerScroll = getScroll(container);
					var unscrollableX = region.right < (containerRect.left - containerScroll.x);
					var unscrollableY = region.bottom < (containerRect.top - containerScroll.y);
					if ((unscrollableX && containerOverflow.x !== 'visible')
							|| (unscrollableY && containerOverflow.x !== 'visible')) {
						return 'hidden';
					}

					var containerState = getOverflowState(container);
					return containerState === 'hidden' ? containerState
							: 'scroll';
				}

				// Check 'overflow': if an element is to the right or below a
				// container
				var overflowsX = region.left >= containerRect.left
						+ containerRect.width;
				var overflowsY = region.top >= containerRect.top
						+ containerRect.height;
				if ((overflowsX && containerOverflow.x === 'hidden')
						|| (overflowsY && containerOverflow.y === 'hidden')) {
					return 'hidden';
				}

				if ((overflowsX && containerOverflow.x !== 'visible')
						|| (overflowsY && containerOverflow.y !== 'visible')) {
					// If the element has fixed position and falls outside the
					// scrollable area
					// of the document, then it is hidden.
					if (treatAsFixedPosition) {
						var docScroll = getScroll(container);
						if (region.left >= (htmlElem.scrollWidth - docScroll.x)
								|| region.right >= (htmlElem.scrollHeight - docScroll.y)) {
							return 'hidden';
						}
					}

					// If the element can be scrolled into view of the parent,
					// it
					// has a scroll
					// state; unless the parent itself is entirely hidden by
					// overflow, in
					// which it is also hidden by overflow.
					var containerState = getOverflowState(container);
					return containerState === 'hidden' ? containerState
							: 'scroll';
				}
			}

			// Does not overflow any ancestor.
			return 'none';
		}

		/**
		 * Returns absolute position of some element within document.
		 * 
		 * @param {HTMLElement}
		 *            element The element to retrieve border width from.
		 * @return {Object} The absolute position (x, y).
		 */
		function getAbsolutePosition(element) {
			var doc = element && element.ownerDocument, docElem = doc
					&& doc.documentElement, res = {
				x : 0,
				y : 0,
				height : 0,
				width : 0,
				sx : 0,
				sy : 0
			}, box;

			if (element) {
				if (element === doc.body) {
					res.x = element.offsetLeft;
					res.y = element.offsetTop;
					res.height = element.offsetHeight;
					res.width = element.offsetWidth;
				} else {
					try {
						box = element.getBoundingClientRect();
					} catch (e) {
						// ignore
					}

					if (box) {
						var body = doc.body, win = doc.defaultView, clientTop = docElem.clientTop
								|| body.clientTop || 0, clientLeft = docElem.clientLeft
								|| body.clientLeft || 0, scrollTop = Math
								.round(win.scrollY)
								|| docElem.scrollTop || body.scrollTop, scrollLeft = Math
								.round(win.scrollX)
								|| docElem.scrollLeft || body.scrollLeft;

						res.x = box.left + scrollLeft - clientLeft;
						res.y = box.top + scrollTop - clientTop;
						res.height = box.height;
						res.width = box.width;
						res.sx = scrollLeft;
						res.sy = scrollTop;
					}
				}
			}

			return res;
		}

		/**
		 * Checks if the given HTML element is visible.
		 * 
		 * @param {Element}
		 *            element The HTML element to be checked.
		 * @return {Boolean} TRUE if the given HTML element is visible, FALSE
		 *         otherwise.
		 */
		function isVisible(element) {
			function isOverflowHidden(e) {
				return getOverflowState(e) === 'hidden'
						&& Array.prototype.every.call(e.childNodes,
								function(e) {
									return e.nodeType !== 1
											|| isOverflowHidden(e)
											|| !consumesSpace(e);
								});
			}

			if (!element) {
				return false;
			}

			var el = element;
			while (el.parentNode && !(el.nodeType === 1 || el.nodeType === 9)) {
				el = el.parentNode;
			}

			// DOCUMENT node
			if (el.nodeType === 9) {
				return true;
			}

			switch (el.nodeName.toUpperCase()) {
			case 'BODY':
				return true;
			case 'META':
			case 'SCRIPT':
				return false;

			case 'INPUT':
				if (el.getAttribute('type') === 'hidden') {
					return false;
				}
				break;

			case 'A':
				if (el.childElementCount === 0) {
					if (/^[ \f\n\r\t\v​\u1680​\u180e\u2000​\u2001\u2002​\u2003\u2004​\u2005\u2006​\u2008​\u2009\u200a​\u2028\u2029​\u205f​\u3000]*$/
							.test(el.textContent)) {
						return false;
					}
				}
				break;

			case 'OPTION':
			case 'OPTGROUP':
				while ((el = el.parentNode)) {
					if (el.nodeType === 1
							&& el.nodeName.toUpperCase() === 'SELECT') {
						break;
					}
				}
				return el && isVisible(el);
			case 'BR':
				el = getParentElement(el);

				return el && isVisible(el);
			default:
				break;
			}

			if (/^hidden|collapsed$/.test(getEffectiveStyle(el, 'visibility'))
					|| !isDisplayed(el) || !consumesSpace(el)) {
				return false;
			}
			return !isOverflowHidden(el);
		}

		/**
		 * Scrolls to the given element.
		 * 
		 * @param {Element}
		 *            element the element to scroll to
		 */
		function scrollToElement(element) {
			var e = element;
			while (e && e.ownerDocument) {
				let pos = getAbsolutePosition(e);
				if (!isWithinViewPort(e, pos)) {
					e.ownerDocument.defaultView.scrollTo(Math
							.max(pos.x - 10, 0), Math.max(pos.y
							- e.ownerDocument.defaultView.innerHeight / 3, 0));
				}
				e = e.ownerDocument.defaultView.frameElement;
			}
		}

		/**
		 * Checks if the given element is located within the viewport.
		 * 
		 * @param {Element}
		 *            element the element to scroll to
		 * @param {Object}
		 *            position of the element to scroll to
		 * @returns {Boolean} true if the element is located in the viewport
		 */
		function isWithinViewPort(element, position) {
			let res = false;
			if (element && element.ownerDocument) {
				let win = element.ownerDocument.defaultView;
				if ((win.scrollY < position.y)
						&& (win.innerHeight + win.scrollY > position.y)) {
					if ((win.scrollX < position.x)
							&& (win.innerWidth + win.scrollX > position.x)) {
						res = true;
					}
				}
			}
			return res;
		}

		/**
		 * Highlight given element.
		 * 
		 * @param {Element}
		 *            element The element to highlight.
		 * @param {Document}
		 *            baseDocument The base document.
		 * @param {Number}
		 *            duration The highlight duration in milliseconds.
		 * @param {String}
		 *            [oddColor] The color used for odd highlight.
		 * @param {String}
		 *            [evenColor] the color used for even highlight (optional).
		 * @return {Array} The highlight interval and clearing timeout.
		 */
		function highlightElement(element, baseDocument, duration, oddColor, evenColor) {
			return this.highlightAllElements([ element ], baseDocument,	duration, oddColor, evenColor);
		}

		

		/**
		 * Switch highlight style.
		 * 
		 * @param {Document}
		 *            baseDocument The base document.
		 * @param {Boolean}
		 *            oddFlag The odd/even flag.
		 * @param {String}
		 *            oddColor The color used for odd highlight.
		 * @param {String}
		 *            evenColor the color used for even highlight.
		 */
		switchHighlightStyle = function(baseDocument, oddFlag, oddColor, evenColor) {
			[ 'neodymium-highlight-box', 'neodymium-outline-box' ].forEach(function(className) {
				var elements = baseDocument.getElementsByClassName(className);
				for (var i = 0; i < elements.length; i++) {
					var style = elements[i].getAttribute('style');
					if (oddFlag) {
						elements[i].setAttribute('style', style.replace(
								evenColor, oddColor));
					} else {
						elements[i].setAttribute('style', style.replace(
								oddColor, evenColor));
					}
				}
			});
		}

		/**
		 * Highlight a single element.
		 * 
		 * @param {Element}
		 *            element The element to highlight.
		 * @param {Number}
		 *            index The element index.
		 * @param {Boolean}
		 *            printIndex Whether or not to print the element's index.
		 * @param {String}
		 *            oddColor The color used for odd highlight.
		 * @param {String}
		 *            hash The hash used to mark a specific group of elements.
		 */
		highlightSingleElement = function(element, index, printIndex, oddColor,	hash) {
			if (!element) {
				return;
			}
			// hightlight owning SELECT element for OPTION or OPTGROUP elements
			if (/^OPT(ION|GROUP)$/.test(element.nodeName.toUpperCase())) {
//				for (var p = element.parentNode; p;) {
//					if (p.nodeType === 1
//							&& p.nodeName.toUpperCase() === 'SELECT') {
//						element = p;
//						break;
//					}
//				}
				return;
			}

			if (index === 0) {
				scrollToElement(element);
			}

			// retrieve elements position
			var pos = getAbsolutePosition(element);

			// create absolute positioned div element and attach as last child
			// of
			// the body
			var doc = element.ownerDocument, divNode = doc.createElement('div'), parentNode = doc.body, divStyle = 'position:absolute; display:block; border-width:1px; border-style:solid; z-index: 9999; opacity: .75; '
					+ 'top: ' + pos.y + 'px; '
					+ 'left: ' + pos.x + 'px; '
					+ 'width: '	+ element.offsetWidth + 'px; '
					+ 'height: ' + element.offsetHeight	+ 'px; '
					+ 'background-color: '	+ oddColor + '; ' + 'border-color: blue;';

			if (!!printIndex) {
				divStyle += ' font-size: 0.8em; padding-left: 2px; color: magenta; font-weight: bold;';
				divNode.textContent = String(index + 1);
			}

			divNode.setAttribute('style', divStyle);
			hash = hash ? ' ' + hash : '';
			divNode.setAttribute('class', 'neodymium-highlight-box' + hash);
			parentNode.appendChild(divNode);
		}

		/**
		 * Outlines the given element. This is a fallback used for
		 * FRAME/FRAMESET elements only.
		 * 
		 * @param {HTMLElement}
		 *            element The element to outline.
		 * @param {String}
		 *            oddColor The background-color to use for outline.
		 */
		outline = function(element, oddColor) {
			if (!element || !oddColor) {
				return;
			}

			var origBG = getStyle(element, 'background-color') || '', origOutline = getStyle(
					element, 'outline')
					|| '';
			if (origBG === 'transparent') {
				origBG = '';
			}

			element.style.backgroundColor = oddColor;
			element.style.outline = '1px solid blue';

			element.__origBG = origBG;
			element.__origOutline = origOutline;
			element.__origClass = element.getAttribute('class');

			var classAtt = element.__origClass || '';
			if (classAtt.indexOf('neodymium-outline-box') < 0) {
				classAtt += ' neodymium-outline-box';
			}
			element.setAttribute('class', classAtt);
		}

		/**
		 * Highlight the elements given.
		 * 
		 * @param {Array}
		 *            elements The elements to highlight.
		 * @param {Document}
		 *            baseDocument The base document.
		 * @param {Number}
		 *            duration The highlight duration in milliseconds.
		 * @param {String}
		 *            [oddColor] The color used for odd highlight.
		 * @param {String}
		 *            [evenColor] the color used for even highlight.
		 * @return {Array} The highlight interval and clearing timeout.
		 */
		this.highlightAllElements = function(elements, baseDocument, duration, oddColor, evenColor) {
			var _oddColor = oddColor ? oddColor	: 'rgb(255, 255, 90)';
			var _evenColor = evenColor ? evenColor : 'rgb(255, 255, 210)';

			if (elements && elements.length > 0 && baseDocument) {
				var blinking = duration > 260 ? 250 : duration - 10;
				var moreThanOnce = elements.length > 1;
				let hash = new Date().getTime();
				if (blinking > 0) {
					// now highlight the nodes
					var isOneHighlighted = false;
					for (var i = 0; i < elements.length; i++) {
						if (/^FRAME(SET)?$/.test(elements[i].localName
								.toUpperCase())) {
							outline(elements[i], _oddColor);
							isOneHighlighted = true;
						} else {
							if (isVisible(elements[i])) {
							highlightSingleElement(elements[i], i,
									moreThanOnce, _oddColor, hash);
							isOneHighlighted = true;
						}}
					}

					if (isOneHighlighted) {
						var self = this;
						var counter = 0;
						var interval = window.setInterval(function() {
							switchHighlightStyle(baseDocument,
									counter % 2 === 0, _oddColor, _evenColor);
							counter++;
						}, blinking);

						// clear blinking after defined duration.
						var timeout = window.setTimeout(function() {
							self.resetHighlightElements(baseDocument, interval,
									null, hash);
						}, duration);
						return [ interval, timeout ];
					}
				} else if (isVisible(elements[0])) {
					scrollToElement(elements[0]);
				}
			}
			return null;
		}

		/**
		 * Resets an elements highlighting.
		 * 
		 * @param {Document}
		 *            baseDocument The base document.
		 * @param {Number}
		 *            interval The interval to reset.
		 * @param {Number}
		 *            timeout The highlight timeout to reset.
		 * @param {String}
		 *            hash The hash used to find a specific group of elements.
		 */
		this.resetHighlightElements = function(baseDocument, interval, timeout,	hash) {
			if (interval) {
				window.clearInterval(interval);
			}
			if (timeout) {
				window.clearTimeout(timeout);
			}

			let clazz = hash ? hash : 'neodymium-highlight-box';
			var highlightDivs = baseDocument.getElementsByClassName(clazz);

			while (highlightDivs.length > 0) {
				var orgParent = highlightDivs[0].ownerDocument.body;
				orgParent.removeChild(highlightDivs[0]);
				highlightDivs = baseDocument.getElementsByClassName(clazz);
			}

			var outlines = baseDocument
					.getElementsByClassName('neodymium-outline-box');
			for (var i = 0; i < outlines.length; i++) {
				var e = outlines[i], origClass = e.__origClass;

				e.style.backgroundColor = e.__origBG || '';
				e.style.outline = e.__origOutline || '';
				if (origClass) {
					e.setAttribute('class', origClass);
				} else {
					e.removeAttribute('class');
				}
				e.__origBG = e.__origOutline = e.__origClass = null;
			}
		}
	}

	window.NEODYMIUM = new NeodymiumInteractor();
}