/**
 * Neodymium Element Highlighting & Interaction Script
 *
 * This script is injected into browser pages (via Selenide, Selenium WebDriver, Playwright, or custom JS executors)
 * to provide visual feedback and element highlighting during automated test execution.
 *
 * It attaches a singleton instance `window.NEODYMIUM` to the global browser window.
 *
 * @author Xceptance GmbH 2026
 */
if (!window.NEODYMIUM) {
	/**
	 * Main Neodymium interactor class encapsulating DOM inspection, visibility checks,
	 * viewport positioning, and overlay highlight injection.
	 */
	function NeodymiumInteractor() {

		/**
		 * Retrieves the computed CSS style property value for a given DOM element.
		 *
		 * @param {HTMLElement} element - The target DOM element.
		 * @param {string} style - The CSS property name (e.g. 'display', 'visibility').
		 * @returns {string|null} Computed property value, or null if uncomputed or 'auto'.
		 */
		function getStyle(element, style) {
			var value;
			var doc = element.ownerDocument;
			if (doc && doc.defaultView && doc.defaultView.getComputedStyle) {
				var css = doc.defaultView.getComputedStyle(element, null);
				value = css ? css.getPropertyValue(style) : null;
			}
			return value === 'auto' ? null : value;
		}

		/**
		 * Computes the effective CSS style value, recursively resolving inherited properties
		 * (e.g. 'inherit') up the DOM element hierarchy.
		 *
		 * @param {HTMLElement} element - The target DOM element.
		 * @param {string} style - The CSS property name to evaluate.
		 * @returns {string|null} The resolved effective CSS property value.
		 */
		function getEffectiveStyle(element, style) {
			var effectiveStyle = getStyle(element, style);
			if ('inherit' === effectiveStyle && element.parentNode) {
				return getEffectiveStyle(element.parentNode, style);
			}
			return effectiveStyle;
		}

		/**
		 * Traverses upwards to find the closest ancestor Node that is an Element (nodeType === 1).
		 * Skips text nodes (3), comment nodes (8), and document fragments (11).
		 *
		 * @param {Node} a - The starting DOM node.
		 * @returns {HTMLElement|null} The parent Element, or null if root document is reached.
		 */
		function getParentElement(a) {
			for (a = a.parentNode; a && a.nodeType != 1 && a.nodeType != 9
					&& a.nodeType != 11;) {
				a = a.parentNode;
			}
			return !!a && a.nodeType === 1 ? a : null;
		}

		/**
		 * Calculates the bounding client rectangle of an element relative to the viewport.
		 * Handles HTML root element fallbacks and catches potential cross-origin or detached node exceptions.
		 *
		 * @param {HTMLElement} element - The target DOM element.
		 * @returns {{left: number, top: number, width: number, height: number, right: number, bottom: number}} Rect object.
		 */
		function getClientRect(element) {
			var box;

			// Special handling for the HTML root element
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
				// Fallback zero rect if getBoundingClientRect fails (e.g., detached iframe node)
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

		/**
		 * Recursively checks if an element or any of its parent ancestors has 'display: none'.
		 *
		 * @param {HTMLElement} element - The target DOM element.
		 * @returns {boolean} True if displayed, false if any ancestor has display: none.
		 */
		function isDisplayed(element) {
			var display = getEffectiveStyle(element, 'display');
			if ('none' === display) {
				return false;
			}
			element = getParentElement(element);
			return !element || isDisplayed(element);
		}

		/**
		 * Checks if an element occupies physical layout space (has non-zero width/height, SVG stroke width,
		 * or non-hidden child nodes that consume space).
		 *
		 * @param {HTMLElement} element - The DOM element to inspect.
		 * @returns {boolean} True if the element or its content occupies layout dimensions.
		 */
		function consumesSpace(element) {
			var box = getClientRect(element), a;
			if (box.height > 0 && box.width > 0) {
				return true;
			}
			// SVG PATH elements may have 0 fill box but consume layout via stroke width
			if (element.nodeType === 1
					&& element.nodeName.toUpperCase() === 'PATH'
					&& (box.height > 0 || box.width > 0)) {
				a = getEffectiveStyle(element, 'stroke-width');
				return !!a && parseInt(a, 10) > 0;
			}
			// Container elements with overflow != hidden might contain visible child nodes
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

		/**
		 * Evaluates whether an element is scrolled out of view or clipped by any overflow: hidden/scroll ancestor.
		 *
		 * @param {HTMLElement} elem - The DOM element to evaluate.
		 * @returns {'hidden'|'scroll'|'none'} 'hidden' if fully clipped, 'scroll' if scrollable into view, 'none' if visible.
		 */
		function getOverflowState(elem) {
			var region = getClientRect(elem);
			var ownerDoc = elem.ownerDocument || elem.document;
			var htmlElem = ownerDoc.documentElement;
			var bodyElem = ownerDoc.body;
			var htmlOverflowStyle = getEffectiveStyle(htmlElem, 'overflow');
			var treatAsFixedPosition;

			/**
			 * Finds the closest overflow container ancestor that could clip child elements.
			 */
			function getOverflowParent(e) {
				function canBeOverflowed(container) {
					if (container == htmlElem) {
						return true;
					}
					var containerDisplay = getEffectiveStyle(container, 'display');
					if (/^inline/.test(containerDisplay)) {
						return false;
					}
					if (position === 'absolute'
							&& getEffectiveStyle(container, 'position') === 'static') {
						return false;
					}
					return true;
				}

				var position = getEffectiveStyle(e, 'position');
				if (position === 'fixed') {
					treatAsFixedPosition = true;
					return e === htmlElem ? null : htmlElem;
				} else {
					var parent = getParentElement(e);
					while (parent && !canBeOverflowed(parent)) {
						parent = getParentElement(parent);
					}
					return parent;
				}
			}

			/**
			 * Resolves effective overflow-x and overflow-y styles for a container element.
			 */
			function getOverflowStyles(e) {
				var overflowElem = e;
				if (htmlOverflowStyle === 'visible') {
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

				if (e === htmlElem) {
					overflow.x = overflow.x === 'visible' ? 'auto' : overflow.x;
					overflow.y = overflow.y === 'visible' ? 'auto' : overflow.y;
				}

				return overflow;
			}

			/**
			 * Retrieves scroll offsets for document or element containers.
			 */
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

			// Check ancestor overflow boundaries
			for (var container = getOverflowParent(elem); !!container; container = getOverflowParent(container)) {
				var containerOverflow = getOverflowStyles(container);

				if (containerOverflow.x === 'visible'
						&& containerOverflow.y === 'visible') {
					continue;
				}

				var containerRect = getClientRect(container);

				if (containerRect.width === 0 || containerRect.height === 0) {
					return 'hidden';
				}

				var underflowsX = region.right < containerRect.left;
				var underflowsY = region.bottom < containerRect.top;
				if ((underflowsX && containerOverflow.x === 'hidden')
						|| (underflowsY && containerOverflow.y === 'hidden')) {
					return 'hidden';
				}

				if ((underflowsX && containerOverflow.x !== 'visible')
						|| (underflowsY && containerOverflow.y !== 'visible')) {

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
					if (treatAsFixedPosition) {
						var docScroll = getScroll(container);
						if (region.left >= (htmlElem.scrollWidth - docScroll.x)
								|| region.right >= (htmlElem.scrollHeight - docScroll.y)) {
							return 'hidden';
						}
					}

					var containerState = getOverflowState(container);
					return containerState === 'hidden' ? containerState
							: 'scroll';
				}
			}

			return 'none';
		}

		/**
		 * Calculates the absolute position (x, y, width, height, scroll offsets) of an element
		 * relative to the top-left of the overall document.
		 *
		 * @param {HTMLElement} element - The target element.
		 * @returns {{x: number, y: number, width: number, height: number, sx: number, sy: number}} Absolute position metrics.
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
						// ignore errors
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
		 * Determines whether a given DOM element is rendered and visible on screen.
		 * Filters out script tags, meta tags, hidden input fields, empty anchors, hidden options, and zero-size elements.
		 *
		 * @param {HTMLElement} element - The element to test.
		 * @returns {boolean} True if visible, false otherwise.
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
		 * Automatically scrolls the browser window to align with the specified element if it falls outside the viewport.
		 * Handles nested frame scrolling recursively.
		 *
		 * @param {HTMLElement} element - The target element to scroll to.
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
		 * Checks if the specified position is currently within the active window viewport.
		 *
		 * @param {HTMLElement} element - Target element.
		 * @param {{x: number, y: number}} position - Absolute position coordinates.
		 * @returns {boolean} True if within current window scroll boundaries.
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
		 * Highlights a single element.
		 *
		 * @param {HTMLElement} element - The element to highlight.
		 * @param {Document} baseDocument - The document context.
		 * @param {number} duration - Blink duration.
		 * @param {string} [oddColor] - Color for odd blink states.
		 * @param {string} [evenColor] - Color for even blink states.
		 * @returns {*} Return value from highlightAllElements.
		 */
		function highlightElement(element, baseDocument, duration, oddColor, evenColor) {
			return this.highlightAllElements([ element ], baseDocument, duration, oddColor, evenColor);
		}

		/**
		 * Toggles opacity and border styling on active highlight overlay divs during blinking intervals.
		 *
		 * @param {Document} baseDocument - Document context containing highlight overlay elements.
		 * @param {boolean} oddFlag - True for odd blink cycle, false for even blink cycle.
		 * @param {string} oddColor - Background color for odd state.
		 * @param {string} evenColor - Background color for even state.
		 */
		switchHighlightStyle = function(baseDocument, oddFlag, oddColor, evenColor) {
			var highlights = baseDocument.getElementsByClassName('neodymium-highlight-box');
			for (var i = 0; i < highlights.length; i++) {
				highlights[i].style.opacity = oddFlag ? '0.75' : '0';
			}
			
			var outlines = baseDocument.getElementsByClassName('neodymium-outline-box');
			for (var i = 0; i < outlines.length; i++) {
				if (oddFlag) {
					outlines[i].style.backgroundColor = oddColor;
					outlines[i].style.outline = '1px solid blue';
				} else {
					outlines[i].style.backgroundColor = outlines[i].__origBG || '';
					outlines[i].style.outline = outlines[i].__origOutline || '';
				}
			}
		}

		/**
		 * Creates and appends an absolute overlay <div> element directly over a target DOM element to visually highlight it.
		 *
		 * @param {HTMLElement} element - The element to visually highlight.
		 * @param {number} index - Index of element in target set (for index label).
		 * @param {boolean} printIndex - Whether to render index number on overlay box.
		 * @param {string} oddColor - Background color.
		 * @param {string} hash - Unique hash key identifying the highlight group.
		 */
		highlightSingleElement = function(element, index, printIndex, oddColor, hash) {
			if (!element) {
				return;
			}
			if (/^OPT(ION|GROUP)$/.test(element.nodeName.toUpperCase())) {
				return;
			}

			if (index === 0) {
				scrollToElement(element);
			}

			var pos = getAbsolutePosition(element);

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
		 * Fallback outlining method for FRAME and FRAMESET elements.
		 *
		 * @param {HTMLElement} element - Frame element.
		 * @param {string} oddColor - Color.
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
		 * Primary entry point to highlight an array of DOM elements on page with configurable duration and blink cycles.
		 *
		 * @param {HTMLElement[]} elements - Array of elements to highlight.
		 * @param {Document} baseDocument - Document context.
		 * @param {number} duration - Blink cycle duration in milliseconds.
		 * @param {string} [oddColor] - Color for odd blink state.
		 * @param {string} [evenColor] - Color for even blink state.
		 * @param {number} [blinkCount] - Total number of blink cycles (defaults to 3).
		 * @returns {null} Always null.
		 */
		this.highlightAllElements = function(elements, baseDocument, duration, oddColor, evenColor, blinkCount) {
			var _oddColor = oddColor ? oddColor	: 'rgb(255, 255, 90)';
			var _evenColor = evenColor ? evenColor : 'rgb(255, 255, 210)';

			if (elements && elements.length > 0 && baseDocument) {
				var moreThanOnce = elements.length > 1;
				let hash = new Date().getTime();
				
				// Clear any active interval to avoid leaky timers
				if (window.NEODYMIUM && window.NEODYMIUM._activeInterval) {
					window.clearInterval(window.NEODYMIUM._activeInterval);
					window.NEODYMIUM._activeInterval = null;
				}

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
						}
					}
				}

				if (isOneHighlighted && duration > 0) {
					var counter = 0;
					var _blinkCount = (blinkCount !== undefined && blinkCount !== null) ? blinkCount : 3;
					var maxTicks = 2 * _blinkCount - 1;
					var interval = window.setInterval(function() {
						if (counter >= maxTicks) {
							window.clearInterval(interval);
							return;
						}
						switchHighlightStyle(baseDocument,
								counter % 2 !== 0, _oddColor, _evenColor);
						counter++;
					}, duration);

					if (window.NEODYMIUM) {
						window.NEODYMIUM._activeInterval = interval;
					}
				}
			}
			return null;
		}

		/**
		 * Clears all active highlight overlays and restores original frame outlines.
		 *
		 * @param {Document} baseDocument - Document context.
		 * @param {number} [interval] - Specific interval ID to clear.
		 * @param {number} [timeout] - Specific timeout ID to clear.
		 * @param {string} [hash] - Specific group hash class name to clear.
		 */
		this.resetHighlightElements = function(baseDocument, interval, timeout, hash) {
			if (interval) {
				window.clearInterval(interval);
			}
			if (window.NEODYMIUM && window.NEODYMIUM._activeInterval) {
				window.clearInterval(window.NEODYMIUM._activeInterval);
				window.NEODYMIUM._activeInterval = null;
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

		// Export visibility check helpers
		this.isVisible = isVisible;
		this.consumesSpace = consumesSpace;
		this.isDisplayed = isDisplayed;
		this.getOverflowState = getOverflowState;
	}

	window.NEODYMIUM = new NeodymiumInteractor();
}
