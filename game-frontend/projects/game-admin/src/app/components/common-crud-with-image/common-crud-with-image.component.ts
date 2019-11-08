import { Component, OnInit, ContentChild, TemplateRef, ViewChild } from '@angular/core';

import { EntityWithImage, CommonEntity } from '@owge/core';
import { AbstractCrudService, ImageStore, ImageSelectorComponent } from '@owge/universe';

import { CommonCrudComponent } from '../common-crud/common-crud.component';

@Component({
  selector: 'app-common-crud-with-image',
  templateUrl: './common-crud-with-image.component.html',
  styleUrls: ['./common-crud-with-image.component.less']
})
export class CommonCrudWithImageComponent<K, T extends CommonEntity<K> & EntityWithImage> extends CommonCrudComponent<K, T>
  implements OnInit {

  @ContentChild('modalBody', { static: true }) public innerModalBody: TemplateRef<any>;
  public viewExposedService: AbstractCrudService<T, K>;

  public ngOnInit() {
    this.viewExposedService = this._crudService;
  }

  /**
   * Defines the image for the input <i>newElement</i>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  newElement
   * @param  image
   */
  public setImage(newElement: T, image: ImageStore): void {
    newElement.image = image.id;
    newElement.imageUrl = image.url;
  }
}