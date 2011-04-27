/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */
package org.geomajas.widget.searchandfilter.client.widget.search;

import java.util.HashSet;
import java.util.Set;

import org.geomajas.widget.searchandfilter.client.SearchAndFilterMessages;
import org.geomajas.widget.searchandfilter.client.util.DataCallback;
import org.geomajas.widget.searchandfilter.client.util.FavouritesCommService;
import org.geomajas.widget.searchandfilter.client.widget.search.SearchWidget.FavouriteRequestHandler;
import org.geomajas.widget.searchandfilter.search.dto.SearchFavourite;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Controller to handle the searches.
 *
 * @see SearchWidgetRegistry.
 * @author Kristof Heirwegh
 */
public class FavouritesController implements FavouriteRequestHandler {

	private final SearchAndFilterMessages messages = GWT.create(SearchAndFilterMessages.class);

	// private static final String BTN_SAVE_IMG = "[ISOMORPHIC]/geomajas/osgeo/save1.png";
	private static final String BTN_ADD_IMG = "[SKIN]/actions/add.png";
	private static final String BTN_CANCEL_IMG = "[ISOMORPHIC]/geomajas/osgeo/undo.png";

	private final Set<FavouriteChangeHandler> favouriteHandlers = new HashSet<FavouriteChangeHandler>();

	// ----------------------------------------------------------

	public void onAddRequested(final FavouriteEvent event) {
		final SearchFavourite fav = event.getNewFav();
		final Window addWindow = new Window();
		addWindow.setTitle(messages.favouritesControllerAddTitle());
		addWindow.setWidth(310);
		addWindow.setHeight(145);
		addWindow.setAutoCenter(true);
		addWindow.setShowMinimizeButton(false);
		addWindow.setIsModal(true);
		//addWindow.setShowModalMask(true);

		VLayout mainLayout = new VLayout(10);
		mainLayout.setMargin(10);

		// ----------------------------------------------------------

		final DynamicForm form = new DynamicForm();
		form.setGroupTitle(messages.favouritesControllerAddGroupTitle());
		form.setIsGroup(true);
		final TextItem nameItem = new TextItem();
		nameItem.setWidth(190);
		nameItem.setTitle(messages.favouritesControllerAddName());
		nameItem.setTooltip(messages.favouritesControllerAddNameTooltip());
		nameItem.setRequired(true);
		final CheckboxItem sharedItem = new CheckboxItem();
		sharedItem.setTitle(messages.favouritesControllerAddShared());
		sharedItem.setTooltip(messages.favouritesControllerAddSharedTooltip());
		form.setFields(nameItem, sharedItem);

		// ----------------------------------------------------------

		HLayout buttonlayout = new HLayout(10);
		buttonlayout.setHeight(20);
		buttonlayout.setWidth100();
		IButton addBtn = new IButton(messages.favouritesControllerAddAdd());
		addBtn.setIcon(BTN_ADD_IMG);
		addBtn.setAutoFit(true);
		addBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (form.validate()) {
					addWindow.hide();
					fav.setName(nameItem.getValueAsString());
					fav.setShared(sharedItem.getValueAsBoolean());
					FavouritesCommService.saveSearchFavourite(fav, new DataCallback<SearchFavourite>() {
						public void execute(SearchFavourite result) {
							fireAddEvent(new FavouriteEvent(null, result, FavouritesController.this));
							addWindow.destroy();
						}
					});
				}
			}
		});
		IButton cancelBtn = new IButton(messages.favouritesControllerAddCancel());
		cancelBtn.setIcon(BTN_CANCEL_IMG);
		cancelBtn.setAutoFit(true);
		cancelBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addWindow.destroy();
			}
		});
		LayoutSpacer lsr = new LayoutSpacer();
		lsr.setWidth("*");

		// ----------------------------------------------------------

		buttonlayout.addMember(lsr);
		buttonlayout.addMember(addBtn);
		buttonlayout.addMember(cancelBtn);
		mainLayout.addMember(form);
		mainLayout.addMember(buttonlayout);
		addWindow.addItem(mainLayout);
		addWindow.show();
	}

	public void onDeleteRequested(final FavouriteEvent event) {
		FavouritesCommService.deleteSearchFavourite(event.getOldFav(), new DataCallback<Boolean>() {
			public void execute(Boolean result) {
				if (result) {
					fireDeleteEvent(new FavouriteEvent(event.getOldFav(), null, FavouritesController.this));
				} else {
					SC.say(messages.favouritesControllerAddCrudError());
				}
			}
		});
	}

	public void onChangeRequested(final FavouriteEvent event) {
		FavouritesCommService.saveSearchFavourite(event.getNewFav(), new DataCallback<SearchFavourite>() {
			public void execute(SearchFavourite result) {
				if (result != null) {
					fireChangeEvent(new FavouriteEvent(event.getOldFav(), result, FavouritesController.this));
				} else {
					SC.say(messages.favouritesControllerAddCrudError());
				}
			}
		});
	}

	// ----------------------------------------------------------

	public void addFavouriteChangeHandler(FavouriteChangeHandler handler) {
		favouriteHandlers.add(handler);
	}

	public void removeFavouriteChangeHandler(FavouriteChangeHandler handler) {
		favouriteHandlers.remove(handler);
	}

	// ----------------------------------------------------------

	private void fireAddEvent(FavouriteEvent event) {
		for (FavouriteChangeHandler handler : favouriteHandlers) {
			handler.onAdd(event);
		}
	}

	private void fireDeleteEvent(FavouriteEvent event) {
		for (FavouriteChangeHandler handler : favouriteHandlers) {
			handler.onDelete(event);
		}
	}

	private void fireChangeEvent(FavouriteEvent event) {
		for (FavouriteChangeHandler handler : favouriteHandlers) {
			handler.onChange(event);
		}
	}

	// ----------------------------------------------------------

	/**
	 * @author Kristof Heirwegh
	 */
	public interface FavouriteChangeHandler {
		void onAdd(FavouriteEvent event);
		void onDelete(FavouriteEvent event);
		void onChange(FavouriteEvent event);
	}

	/**
	 * @author Kristof Heirwegh
	 */
	public static class FavouriteEvent {
		private SearchFavourite oldFav;
		private SearchFavourite newFav;
		private Object source;

		public FavouriteEvent(SearchFavourite oldFav, SearchFavourite newFav, Object source) {
			this.oldFav = oldFav;
			this.newFav = newFav;
			this.source = source;
		}

		public SearchFavourite getOldFav() {
			return oldFav;
		}

		public void setOldFav(SearchFavourite oldFav) {
			this.oldFav = oldFav;
		}

		public SearchFavourite getNewFav() {
			return newFav;
		}

		public void setNewFav(SearchFavourite newFav) {
			this.newFav = newFav;
		}

		public Object getSource() {
			return source;
		}

		public void setSource(Object source) {
			this.source = source;
		}
	}
}
